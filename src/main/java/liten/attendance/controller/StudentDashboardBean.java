package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.domain.Student;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("studentDashboardBean")
@SessionScoped
public class StudentDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private Student currentStudent;

    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_3";

    // Stats perso (filtrées par term/period)
    private int totalSessions;
    private int totalPresents;
    private int totalAbsences;
    private int totalJustified;
    private double presenceRate;
    private double schoolRate;
    private double globalRate;

    // Stats école (moyenne)
    private double schoolAvgPresenceRate;

    // Listes
    private List<CourseAbsenceInfo> courseAbsences = new ArrayList<>();
    private List<ActionItem> advisorActions = new ArrayList<>();
    private List<JustificationItem> myJustifications = new ArrayList<>();
    private List<Object[]> recentNotifications = new ArrayList<>();

    @PostConstruct
    public void init() {
        resolveCurrentStudent();
        if (currentStudent != null) {
            loadAll();
        }
    }

    private void loadAll() {
        loadOverview();
        loadSchoolStats();
        loadCourseAbsences();
        loadAdvisorActions();
        loadMyJustifications();
        loadNotifications();
    }

    private void resolveCurrentStudent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID == null) return;

        EntityManager em = DataManager.getEntityManager();
        try {
            currentStudent = em.find(Student.class, personID);
        } finally {
            em.close();
        }
    }

    private void loadOverview() {
        EntityManager em = DataManager.getEntityManager();
        try {
            Object[] r = (Object[]) em.createNativeQuery(
                    "SELECT COUNT(*), SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.studentID = ? "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, currentStudent.getStudentID())
                    .getSingleResult();
            totalSessions = r[0] != null ? ((Number) r[0]).intValue() : 0;
            totalPresents = r[1] != null ? ((Number) r[1]).intValue() : 0;
            totalAbsences = totalSessions - totalPresents;
            presenceRate = totalSessions > 0 ? (100.0 * totalPresents / totalSessions) : 0;

            totalJustified = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM absencejustification "
                    + "WHERE studentID = ? AND workflowStatus = 'Approved'")
                    .setParameter(1, currentStudent.getStudentID())
                    .getSingleResult()).intValue();
            
                               // Taux de l'école de l'étudiant (SSE ou BSS)
Object[] schoolRow = (Object[]) em.createNativeQuery(
        "SELECT COUNT(*) AS total, SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END) AS presents "
        + "FROM studentattendance sa "
        + "JOIN student s ON s.studentID = sa.studentID "
        + "WHERE s.school = ?")
        .setParameter(1, currentStudent.getSchool())
        .getSingleResult();
int schoolTotal = schoolRow[0] != null ? ((Number) schoolRow[0]).intValue() : 0;
int schoolPresents = schoolRow[1] != null ? ((Number) schoolRow[1]).intValue() : 0;
schoolRate = schoolTotal > 0 ? (100.0 * schoolPresents / schoolTotal) : 0;

// Taux global toutes écoles
Object[] globalRow = (Object[]) em.createNativeQuery(
        "SELECT COUNT(*) AS total, SUM(CASE WHEN isPresent=1 THEN 1 ELSE 0 END) AS presents "
        + "FROM studentattendance")
        .getSingleResult();
int globalTotal = globalRow[0] != null ? ((Number) globalRow[0]).intValue() : 0;
int globalPresents = globalRow[1] != null ? ((Number) globalRow[1]).intValue() : 0;
globalRate = globalTotal > 0 ? (100.0 * globalPresents / globalTotal) : 0;
            
            
            
        } finally {
            em.close();
        }
    }

    private void loadSchoolStats() {
        EntityManager em = DataManager.getEntityManager();
        try {
            Object[] r = (Object[]) em.createNativeQuery(
                    "SELECT COUNT(*), SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE s.school = ? "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, currentStudent.getSchool())
                    .getSingleResult();
            int tot = r[0] != null ? ((Number) r[0]).intValue() : 0;
            int pre = r[1] != null ? ((Number) r[1]).intValue() : 0;
            schoolAvgPresenceRate = tot > 0 ? (100.0 * pre / tot) : 0;
        } catch (Exception e) {
            schoolAvgPresenceRate = 0;
        } finally {
            em.close();
        }
    }

  private void loadCourseAbsences() {
    courseAbsences.clear();
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT c.code, c.name, COUNT(*) AS nbAbs, "
                + "GROUP_CONCAT(a.attendanceDate ORDER BY a.attendanceDate SEPARATOR ', ') AS dates "
                + "FROM studentattendance sa "
                + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                + "JOIN course c ON c.courseID = a.courseID "
                + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                + "WHERE sa.studentID = ? AND sa.isPresent = 0 "
                + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                + "GROUP BY c.code, c.name ORDER BY nbAbs DESC")
                .setParameter(1, selectedTermID)
                .setParameter(2, selectedPeriodID)
                .setParameter(3, currentStudent.getStudentID())
                .getResultList();

        for (Object[] row : rows) {
            int nb = ((Number) row[2]).intValue();
            String alert = "OK";
            String color = "#16a34a";
            if (nb >= 5) { alert = "EXCLUSION"; color = "#dc2626"; }
            else if (nb == 4) { alert = "CONVOCATION"; color = "#f59e0b"; }
            else if (nb == 3) { alert = "ALERTE PARENT"; color = "#3b82f6"; }
            String dates = row[3] != null ? (String) row[3] : "";
            courseAbsences.add(new CourseAbsenceInfo(
                    (String) row[0], (String) row[1], nb, alert, color, dates));
        }
    } finally {
        em.close();
    }
}

    private void loadAdvisorActions() {
    advisorActions.clear();
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT act.name, act.actionContent, act.nbrAbsences, "
                + "  act.isCompleted, act.actionDate, c.code "
                + "FROM action act "
                + "LEFT JOIN course c ON c.courseID = act.courseID "
                + "WHERE act.studentID = ? "
                + "ORDER BY act.actionDate DESC")
                .setParameter(1, currentStudent.getStudentID())
                .getResultList();

        for (Object[] r : rows) {
            // Robuste : gère Boolean, Number, et String pour isCompleted
            boolean done = false;
            if (r[3] != null) {
                String s = r[3].toString();
                done = s.equals("1") || s.equalsIgnoreCase("true");
            }

            int nbAbs = 0;
            if (r[2] != null) {
                try { nbAbs = ((Number) r[2]).intValue(); } catch (Exception ex) {}
            }

            String dateStr = "";
            if (r[4] != null) {
                String s = r[4].toString();
                dateStr = s.length() >= 10 ? s.substring(0, 10) : s;
            }

            advisorActions.add(new ActionItem(
                    (String) r[0],
                    (String) r[1],
                    nbAbs,
                    done,
                    dateStr,
                    (String) r[5]));
        }
    } catch (Exception e) {
        e.printStackTrace(); // au lieu de silencieux, on log pour debug futur
    } finally {
        em.close();
    }
}

    private void loadMyJustifications() {
        myJustifications.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT aj.workflowStatus, aj.reasons, aj.startDate, aj.createdDate, "
                    + "  c.code, aj.advisorComments, aj.facultyComments "
                    + "FROM absencejustification aj "
                    + "LEFT JOIN course c ON c.courseID = aj.courseID "
                    + "WHERE aj.studentID = ? "
                    + "ORDER BY aj.createdDate DESC LIMIT 20")
                    .setParameter(1, currentStudent.getStudentID())
                    .getResultList();

            for (Object[] r : rows) {
                String status = (String) r[0];
                String statusColor;
                switch (status != null ? status : "") {
                    case "Approved": statusColor = "#16a34a"; break;
                    case "Rejected": statusColor = "#dc2626"; break;
                    case "AdvisorRejected": statusColor = "#dc2626"; break;
                    case "AdvisorApproved": statusColor = "#3b82f6"; break;
                    default: statusColor = "#f59e0b";
                }
                myJustifications.add(new JustificationItem(
                        status,
                        (String) r[1],
                     r[2] != null ? r[2].toString().substring(0, Math.min(10, r[2].toString().length())) : "",
r[3] != null ? r[3].toString().replace('T', ' ').substring(0, Math.min(16, r[3].toString().length())) : "",
                        (String) r[4],
                        (String) r[5],
                        (String) r[6],
                        statusColor));
            }
        } finally {
            em.close();
        }
    }

    private void loadNotifications() {
        recentNotifications.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT notifType, subject, sentDate FROM notification "
                    + "WHERE studentID = ? ORDER BY sentDate DESC LIMIT 10")
                    .setParameter(1, currentStudent.getStudentID())
                    .getResultList();
            recentNotifications = rows;
        } finally {
            em.close();
        }
    }

    public String refresh() {
        if (currentStudent != null) loadAll();
        return null;
    }

    public Student getCurrentStudent() { return currentStudent; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; if (currentStudent != null) loadAll(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; if (currentStudent != null) loadAll(); }
    public int getTotalSessions() { return totalSessions; }
    public int getTotalPresents() { return totalPresents; }
    public int getTotalAbsences() { return totalAbsences; }
    public int getTotalJustified() { return totalJustified; }
    public double getPresenceRate() { return presenceRate; }
    public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", presenceRate); }
    public double getSchoolAvgPresenceRate() { return schoolAvgPresenceRate; }
    public String getFormattedSchoolRate() { return String.format(java.util.Locale.US, "%.1f%%", schoolAvgPresenceRate); }
    public int getDiffWithSchool() { return (int) Math.round(presenceRate - schoolAvgPresenceRate); }
    public List<CourseAbsenceInfo> getCourseAbsences() { return courseAbsences; }
    public List<ActionItem> getAdvisorActions() { return advisorActions; }
    public List<JustificationItem> getMyJustifications() { return myJustifications; }
    public List<Object[]> getRecentNotifications() { return recentNotifications; }

    public static class CourseAbsenceInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String code, name, alert, color, dates;
    private final int nbAbsences;
    public CourseAbsenceInfo(String c, String n, int nb, String a, String col, String d) {
        this.code = c; this.name = n; this.nbAbsences = nb;
        this.alert = a; this.color = col; this.dates = d;
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getNbAbsences() { return nbAbsences; }
    public String getAlert() { return alert; }
    public String getColor() { return color; }
    public String getDates() { return dates; }
}

    public static class ActionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name, content;
        private final int nbAbsences;
        private final boolean completed;
        private final String actionDate, courseCode;
        public ActionItem(String n, String c, int nb, boolean d, String ad, String cc) {
            this.name = n; this.content = c; this.nbAbsences = nb;
            this.completed = d; this.actionDate = ad; this.courseCode = cc;
        }
        public String getName() { return name; }
        public String getContent() { return content; }
        public int getNbAbsences() { return nbAbsences; }
        public boolean isCompleted() { return completed; }
        public String getActionDate() { return actionDate; }
        public String getCourseCode() { return courseCode; }
        public String getStatusBadge() { return completed ? "✓ Terminée" : "⏳ En cours"; }
        public String getStatusColor() { return completed ? "#16a34a" : "#f59e0b"; }
    }

    public static class JustificationItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String status, reasons, startDate, createdDate, courseCode;
        private final String advisorComments, facultyComments, statusColor;
        public JustificationItem(String s, String r, String sd, String cd, String cc, String ac, String fc, String col) {
            this.status = s; this.reasons = r; this.startDate = sd; this.createdDate = cd;
            this.courseCode = cc; this.advisorComments = ac; this.facultyComments = fc; this.statusColor = col;
        }
        public String getStatus() { return status; }
        public String getReasons() { return reasons; }
        public String getStartDate() { return startDate; }
        public String getCreatedDate() { return createdDate; }
        public String getCourseCode() { return courseCode; }
        public String getAdvisorComments() { return advisorComments; }
        public String getFacultyComments() { return facultyComments; }
        public String getStatusColor() { return statusColor; }        
                
        
        
    }
    
    public double getSchoolRate() { return schoolRate; }
public String getFormattedSchoolRate2() { return String.format(java.util.Locale.US, "%.1f%%", schoolRate); }
public double getGlobalRate() { return globalRate; }
public String getFormattedGlobalRate() { return String.format(java.util.Locale.US, "%.1f%%", globalRate); }
}