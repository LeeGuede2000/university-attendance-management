package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("parentDashboardBean")
@SessionScoped
public class ParentDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private String parentName;
    private String parentID;

    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_3";

    private List<ChildStat> myChildren = new ArrayList<>();
    private List<ClassAnonStat> classStats = new ArrayList<>();
    private List<NotificationRow> myNotifications = new ArrayList<>();
    private List<AdvisorActionRow> advisorActions = new ArrayList<>();

    @PostConstruct
    public void init() {
        resolveCurrent();
        if (parentID != null) {
            loadAll();
        }
    }

    private void loadAll() {
        myChildren.clear();
        classStats.clear();
        myNotifications.clear();
        advisorActions.clear();
        loadMyChildren();
        loadAnonymizedClassStats();
        loadMyNotifications();
        loadAdvisorActions();
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        parentID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (parentID == null) return;

        EntityManager em = DataManager.getEntityManager();
        try {
            Object[] row = (Object[]) em.createNativeQuery(
                    "SELECT firstName, lastName FROM parent WHERE parentID = ?")
                    .setParameter(1, parentID)
                    .getSingleResult();
            parentName = row[0] + " " + row[1];
        } catch (Exception e) {
            parentName = "Parent";
        } finally {
            em.close();
        }
    }

    private void loadMyChildren() {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.studentID, s.firstName, s.lastName, s.matricule, s.school, "
                    + "  s.academicLevel, sp.relationship "
                    + "FROM parentstudent sp "
                    + "JOIN student s ON s.studentID = sp.studentID "
                    + "WHERE sp.parentID = ? AND sp.stillValid = 1")
                    .setParameter(1, parentID)
                    .getResultList();

            for (Object[] r : rows) {
                String studentID = (String) r[0];
                ChildStat child = new ChildStat(
                        studentID, (String) r[1], (String) r[2],
                        (String) r[3], (String) r[4], (String) r[5],
                        (String) r[6]);
                loadChildPresenceStats(child, studentID);
                loadChildAlerts(child, studentID);
                myChildren.add(child);
            }
        } finally {
            em.close();
        }
    }

    private void loadChildPresenceStats(ChildStat child, String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            Object[] r = (Object[]) em.createNativeQuery(
                    "SELECT COUNT(*), SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.studentID = ? "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, studentID)
                    .getSingleResult();
            int total = r[0] != null ? ((Number) r[0]).intValue() : 0;
            int presents = r[1] != null ? ((Number) r[1]).intValue() : 0;
            child.setTotalSessions(total);
            child.setTotalPresents(presents);
            child.setTotalAbsences(total - presents);
            child.setPresenceRate(total > 0 ? (100.0 * presents / total) : 0);
        } finally {
            em.close();
        }
    }

    private void loadChildAlerts(ChildStat child, String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT c.code, COUNT(*) AS nbAbsences "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN course c ON c.courseID = a.courseID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.studentID = ? AND sa.isPresent = 0 "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY c.code HAVING nbAbsences >= 3 ORDER BY nbAbsences DESC")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, studentID)
                    .getResultList();

            List<ChildCourseAlert> alerts = new ArrayList<>();
            for (Object[] r : rows) {
                int nb = ((Number) r[1]).intValue();
                String level = nb >= 5 ? "EXCLUSION" : (nb == 4 ? "CONVOCATION" : "PARENT_ALERT");
                String color = nb >= 5 ? "#dc2626" : (nb == 4 ? "#f59e0b" : "#3b82f6");
                alerts.add(new ChildCourseAlert((String) r[0], nb, level, color));
            }
            child.setCourseAlerts(alerts);
        } finally {
            em.close();
        }
    }

    /** Stats agrégées par classe SANS NOMS d'étudiants (confidentialité). */
    private void loadAnonymizedClassStats() {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT c.code, c.name, c.school, "
                    + "  COUNT(sa.studentID) AS total, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents "
                    + "FROM course c "
                    + "JOIN attendance a ON a.courseID = c.courseID "
                    + "JOIN studentattendance sa ON sa.attendanceID = a.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY c.code, c.name, c.school "
                    + "HAVING total > 0 "
                    + "ORDER BY c.school, c.code")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .getResultList();

            for (Object[] r : rows) {
                int total = ((Number) r[3]).intValue();
                int presents = ((Number) r[4]).intValue();
                double pct = total > 0 ? (100.0 * presents / total) : 0;
                int absentsPct = total > 0 ? 100 - (int) Math.round(pct) : 0;
                classStats.add(new ClassAnonStat(
                        (String) r[0], (String) r[1], (String) r[2],
                        total, presents, pct, absentsPct));
            }
        } finally {
            em.close();
        }
    }

    private void loadMyNotifications() {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT n.notificationID, n.notifType, n.subject, n.sentDate, "
                    + "  s.firstName, s.lastName, c.code "
                    + "FROM notification n "
                    + "JOIN student s ON s.studentID = n.studentID "
                    + "JOIN course c ON c.courseID = n.courseID "
                    + "WHERE n.studentID IN ("
                    + "  SELECT studentID FROM parentstudent WHERE parentID = ? AND stillValid = 1"
                    + ") "
                    + "ORDER BY n.sentDate DESC LIMIT 20")
                    .setParameter(1, parentID)
                    .getResultList();

            for (Object[] r : rows) {
                String dateStr = r[3] != null
                        ? r[3].toString().substring(0, Math.min(10, r[3].toString().length())) : "";
                myNotifications.add(new NotificationRow(
                        (String) r[0], (String) r[1], (String) r[2],
                        dateStr, r[4] + " " + r[5], (String) r[6]));
            }
        } finally {
            em.close();
        }
    }

    /** Actions correctives du conseiller (advisor) pour mes enfants. */
    private void loadAdvisorActions() {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT act.name, act.actionContent, act.nbrAbsences, "
                    + "  act.isCompleted, act.actionDate, c.code, "
                    + "  s.firstName, s.lastName "
                    + "FROM action act "
                    + "JOIN student s ON s.studentID = act.studentID "
                    + "LEFT JOIN course c ON c.courseID = act.courseID "
                    + "WHERE act.studentID IN ("
                    + "  SELECT studentID FROM parentstudent WHERE parentID = ? AND stillValid = 1"
                    + ") "
                    + "ORDER BY act.actionDate DESC LIMIT 30")
                    .setParameter(1, parentID)
                    .getResultList();

            for (Object[] r : rows) {
                boolean done = false;
                if (r[3] != null) {
                    String s = r[3].toString();
                    done = s.equals("1") || s.equalsIgnoreCase("true");
                }
                String dateStr = r[4] != null
                        ? r[4].toString().substring(0, Math.min(10, r[4].toString().length())) : "";
                int nbAbs = r[2] != null ? ((Number) r[2]).intValue() : 0;
                advisorActions.add(new AdvisorActionRow(
                        (String) r[0], (String) r[1], nbAbs, done,
                        dateStr, (String) r[5], r[6] + " " + r[7]));
            }
        } catch (Exception e) {
            // silent
        } finally {
            em.close();
        }
    }

    public String refresh() {
        if (parentID != null) loadAll();
        return null;
    }

    public String getParentName() { return parentName; }
    public String getParentID() { return parentID; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; if (parentID != null) loadAll(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; if (parentID != null) loadAll(); }
    public List<ChildStat> getMyChildren() { return myChildren; }
    public List<ClassAnonStat> getClassStats() { return classStats; }
    public List<NotificationRow> getMyNotifications() { return myNotifications; }
    public List<AdvisorActionRow> getAdvisorActions() { return advisorActions; }

    public static class ChildStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, firstName, lastName, matricule, school, academicLevel, relationship;
        private int totalSessions, totalPresents, totalAbsences;
        private double presenceRate;
        private List<ChildCourseAlert> courseAlerts = new ArrayList<>();
        public ChildStat(String id, String fn, String ln, String m, String sc, String lv, String rel) {
            this.studentID = id; this.firstName = fn; this.lastName = ln;
            this.matricule = m; this.school = sc; this.academicLevel = lv; this.relationship = rel;
        }
        public String getStudentID() { return studentID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getMatricule() { return matricule; }
        public String getSchool() { return school; }
        public String getAcademicLevel() { return academicLevel; }
        public String getRelationship() { return relationship; }
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int t) { this.totalSessions = t; }
        public int getTotalPresents() { return totalPresents; }
        public void setTotalPresents(int p) { this.totalPresents = p; }
        public int getTotalAbsences() { return totalAbsences; }
        public void setTotalAbsences(int a) { this.totalAbsences = a; }
        public double getPresenceRate() { return presenceRate; }
        public void setPresenceRate(double r) { this.presenceRate = r; }
        public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", presenceRate); }
        public List<ChildCourseAlert> getCourseAlerts() { return courseAlerts; }
        public void setCourseAlerts(List<ChildCourseAlert> c) { this.courseAlerts = c; }
        public boolean isHasAlerts() { return !courseAlerts.isEmpty(); }
    }

    public static class ChildCourseAlert implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String courseCode;
        private final int nbAbsences;
        private final String alertLevel, color;
        public ChildCourseAlert(String c, int n, String l, String col) {
            this.courseCode = c; this.nbAbsences = n;
            this.alertLevel = l; this.color = col;
        }
        public String getCourseCode() { return courseCode; }
        public int getNbAbsences() { return nbAbsences; }
        public String getAlertLevel() { return alertLevel; }
        public String getColor() { return color; }
    }

    public static class ClassAnonStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String code, name, school;
        private final int total, presents, absentsPct;
        private final double pct;
        public ClassAnonStat(String c, String n, String s, int t, int p, double pc, int ap) {
            this.code = c; this.name = n; this.school = s;
            this.total = t; this.presents = p; this.pct = pc; this.absentsPct = ap;
        }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSchool() { return school; }
        public int getTotal() { return total; }
        public int getPresents() { return presents; }
        public double getPct() { return pct; }
        public String getFormattedPct() { return String.format(java.util.Locale.US, "%.1f%%", pct); }
        public int getAbsentsPct() { return absentsPct; }
    }

    public static class NotificationRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String notificationID, notifType, subject, sentDate, studentName, courseCode;
        public NotificationRow(String i, String t, String su, String d, String sn, String cc) {
            this.notificationID = i; this.notifType = t; this.subject = su;
            this.sentDate = d; this.studentName = sn; this.courseCode = cc;
        }
        public String getNotificationID() { return notificationID; }
        public String getNotifType() { return notifType; }
        public String getSubject() { return subject; }
        public String getSentDate() { return sentDate; }
        public String getStudentName() { return studentName; }
        public String getCourseCode() { return courseCode; }
        public String getColor() {
            switch (notifType != null ? notifType : "") {
                case "EXCLUSION": return "#dc2626";
                case "CONVOCATION": return "#f59e0b";
                case "PARENT_ALERT": return "#3b82f6";
                default: return "#94a3b8";
            }
        }
    }

    public static class AdvisorActionRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name, content;
        private final int nbAbsences;
        private final boolean completed;
        private final String actionDate, courseCode, studentName;
        public AdvisorActionRow(String n, String c, int nb, boolean d, String ad, String cc, String sn) {
            this.name = n; this.content = c; this.nbAbsences = nb;
            this.completed = d; this.actionDate = ad; this.courseCode = cc; this.studentName = sn;
        }
        public String getName() { return name; }
        public String getContent() { return content; }
        public int getNbAbsences() { return nbAbsences; }
        public boolean isCompleted() { return completed; }
        public String getActionDate() { return actionDate; }
        public String getCourseCode() { return courseCode; }
        public String getStudentName() { return studentName; }
        public String getStatusBadge() { return completed ? "✓ Terminée" : "⏳ En cours"; }
        public String getStatusColor() { return completed ? "#16a34a" : "#f59e0b"; }
    }
}