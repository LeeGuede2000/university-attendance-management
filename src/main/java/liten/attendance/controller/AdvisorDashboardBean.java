package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.dao.JustificationDAO;
import liten.attendance.domain.Faculty;
import liten.attendance.service.StatisticsService;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("advisorDashboardBean")
@SessionScoped
public class AdvisorDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final JustificationDAO justificationDAO = new JustificationDAO();
    private final StatisticsService statsInstance = new StatisticsService();

    private Faculty currentAdvisor;

    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_3";

    private List<SupervisedStudent> myStudents = new ArrayList<>();
    private int nbSupervised;
    private int nbParentAlerts;
    private int nbConvocations;
    private int nbExclusions;
    private int nbPendingReviews;

    private int currentYearAbsences;
    private int lastYearAbsences;

    private List<StatisticsService.MonthStat> monthStats;
    private List<SchoolPresenceStat> statsBySchool = new ArrayList<>();
    private List<RecentActionRow> recentActions = new ArrayList<>();

    @PostConstruct
    public void init() {
        resolveCurrent();
        if (currentAdvisor != null) {
            loadAll();
        }
    }

    private void loadAll() {
        loadSupervisedStudents();
        loadCounters();
        loadYearlyComparison();
        loadSchoolStats();
        loadRecentActions();
        monthStats = statsInstance.getAbsencesByMonth(selectedTermID, selectedPeriodID);
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID == null) return;
        currentAdvisor = facultyDAO.findById(personID);

        if (currentAdvisor != null && !Boolean.TRUE.equals(currentAdvisor.getIsAdvisor())) {
            currentAdvisor = null;
        }
    }

    private void loadSupervisedStudents() {
        myStudents.clear();
        nbParentAlerts = 0; nbConvocations = 0; nbExclusions = 0;
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.studentID, s.firstName, s.lastName, s.matricule, s.school, s.academicLevel "
                    + "FROM academicadvisor aa "
                    + "JOIN student s ON s.studentID = aa.studentID "
                    + "WHERE aa.facultyID = ?")
                    .setParameter(1, currentAdvisor.getFacultyID())
                    .getResultList();

            for (Object[] r : rows) {
                String studentID = (String) r[0];
                SupervisedStudent stu = new SupervisedStudent(
                        studentID, (String) r[1], (String) r[2],
                        (String) r[3], (String) r[4], (String) r[5]);
                loadStudentDetails(stu, studentID);
                myStudents.add(stu);

                int worstCount = stu.getWorstCourseAbsences();
                if (worstCount >= 5) nbExclusions++;
                else if (worstCount == 4) nbConvocations++;
                else if (worstCount == 3) nbParentAlerts++;
            }
        } finally {
            em.close();
        }
        nbSupervised = myStudents.size();
    }

    private void loadStudentDetails(SupervisedStudent stu, String studentID) {
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
            stu.setTotalSessions(total);
            stu.setTotalPresents(presents);
            stu.setTotalAbsences(total - presents);
            stu.setPresenceRate(total > 0 ? (100.0 * presents / total) : 0);

            @SuppressWarnings("unchecked")
            List<Object[]> alertRows = em.createNativeQuery(
                    "SELECT c.code, COUNT(*) AS nb "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN course c ON c.courseID = a.courseID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.studentID = ? AND sa.isPresent = 0 "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY c.code HAVING nb >= 3 ORDER BY nb DESC")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, studentID)
                    .getResultList();

            List<CourseAlertItem> alerts = new ArrayList<>();
            int worst = 0;
            for (Object[] ar : alertRows) {
                int nb = ((Number) ar[1]).intValue();
                if (nb > worst) worst = nb;
                String lvl = nb >= 5 ? "EXCLUSION" : (nb == 4 ? "CONVOCATION" : "PARENT_ALERT");
                String col = nb >= 5 ? "#dc2626" : (nb == 4 ? "#f59e0b" : "#3b82f6");
                alerts.add(new CourseAlertItem((String) ar[0], nb, lvl, col));
            }
            stu.setCourseAlerts(alerts);
            stu.setWorstCourseAbsences(worst);
        } finally {
            em.close();
        }
    }

    private void loadCounters() {
        nbPendingReviews = justificationDAO.findPendingForAdvisor(currentAdvisor.getFacultyID()).size();
    }

    private void loadYearlyComparison() {
        currentYearAbsences = countAbsencesForPeriod("PER_3");
        lastYearAbsences = countAbsencesForPeriod("PER_2");
    }

    private int countAbsencesForPeriod(String periodID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            Number n = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.periodID = ? "
                    + "WHERE sa.isPresent = 0 "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, periodID)
                    .getSingleResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    private void loadSchoolStats() {
        statsBySchool.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.school, "
                    + "  COUNT(sa.studentID) AS total, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE s.school IS NOT NULL "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.school")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .getResultList();

            for (Object[] r : rows) {
                int total = ((Number) r[1]).intValue();
                int presents = ((Number) r[2]).intValue();
                double pct = total > 0 ? (100.0 * presents / total) : 0;
                statsBySchool.add(new SchoolPresenceStat(
                        (String) r[0], total, presents, pct));
            }
        } finally {
            em.close();
        }
    }

    private void loadRecentActions() {
        recentActions.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT n.notifType, s.firstName, s.lastName, c.code, n.sentDate, n.subject "
                    + "FROM notification n "
                    + "JOIN student s ON s.studentID = n.studentID "
                    + "JOIN course c ON c.courseID = n.courseID "
                    + "WHERE n.studentID IN ("
                    + "  SELECT studentID FROM academicadvisor WHERE facultyID = ?"
                    + ") "
                    + "ORDER BY n.sentDate DESC LIMIT 20")
                    .setParameter(1, currentAdvisor.getFacultyID())
                    .getResultList();

            for (Object[] r : rows) {
                String dateStr = r[4] != null
                        ? r[4].toString().substring(0, Math.min(10, r[4].toString().length())) : "";
                recentActions.add(new RecentActionRow(
                        (String) r[0], r[1] + " " + r[2], (String) r[3],
                        dateStr, (String) r[5]));
            }
        } finally {
            em.close();
        }
    }

    public String refresh() {
        if (currentAdvisor != null) {
            loadAll();
        }
        return null;
    }

    public int getEvolutionPct() {
        if (lastYearAbsences == 0) return 0;
        return (int) Math.round(100.0 * (currentYearAbsences - lastYearAbsences) / lastYearAbsences);
    }

    public Faculty getCurrentAdvisor() { return currentAdvisor; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; if (currentAdvisor != null) loadAll(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; if (currentAdvisor != null) loadAll(); }
    public List<SupervisedStudent> getMyStudents() { return myStudents; }
    public int getNbSupervised() { return nbSupervised; }
    public int getNbParentAlerts() { return nbParentAlerts; }
    public int getNbConvocations() { return nbConvocations; }
    public int getNbExclusions() { return nbExclusions; }
    public int getNbPendingReviews() { return nbPendingReviews; }
    public int getCurrentYearAbsences() { return currentYearAbsences; }
    public int getLastYearAbsences() { return lastYearAbsences; }
    public List<StatisticsService.MonthStat> getMonthStats() { return monthStats; }
    public List<SchoolPresenceStat> getStatsBySchool() { return statsBySchool; }
    public List<RecentActionRow> getRecentActions() { return recentActions; }

    public static class SupervisedStudent implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, firstName, lastName, matricule, school, academicLevel;
        private int totalSessions, totalPresents, totalAbsences, worstCourseAbsences;
        private double presenceRate;
        private List<CourseAlertItem> courseAlerts = new ArrayList<>();
        public SupervisedStudent(String i, String f, String l, String m, String sc, String lv) {
            this.studentID = i; this.firstName = f; this.lastName = l;
            this.matricule = m; this.school = sc; this.academicLevel = lv;
        }
        public String getStudentID() { return studentID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getMatricule() { return matricule; }
        public String getSchool() { return school; }
        public String getAcademicLevel() { return academicLevel; }
        public String getFullName() { return firstName + " " + lastName; }
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int t) { this.totalSessions = t; }
        public int getTotalPresents() { return totalPresents; }
        public void setTotalPresents(int p) { this.totalPresents = p; }
        public int getTotalAbsences() { return totalAbsences; }
        public void setTotalAbsences(int a) { this.totalAbsences = a; }
        public double getPresenceRate() { return presenceRate; }
        public void setPresenceRate(double r) { this.presenceRate = r; }
        public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", presenceRate); }
        public List<CourseAlertItem> getCourseAlerts() { return courseAlerts; }
        public void setCourseAlerts(List<CourseAlertItem> c) { this.courseAlerts = c; }
        public boolean isHasAlerts() { return !courseAlerts.isEmpty(); }
        public int getWorstCourseAbsences() { return worstCourseAbsences; }
        public void setWorstCourseAbsences(int w) { this.worstCourseAbsences = w; }
    }

    public static class CourseAlertItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String courseCode;
        private final int nbAbsences;
        private final String alertLevel, color;
        public CourseAlertItem(String c, int n, String l, String col) {
            this.courseCode = c; this.nbAbsences = n;
            this.alertLevel = l; this.color = col;
        }
        public String getCourseCode() { return courseCode; }
        public int getNbAbsences() { return nbAbsences; }
        public String getAlertLevel() { return alertLevel; }
        public String getColor() { return color; }
    }

    public static class SchoolPresenceStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String school;
        private final int total, presents;
        private final double pct;
        public SchoolPresenceStat(String s, int t, int p, double pc) {
            this.school = s; this.total = t; this.presents = p; this.pct = pc;
        }
        public String getSchool() { return school; }
        public int getTotal() { return total; }
        public int getPresents() { return presents; }
        public double getPct() { return pct; }
        public String getFormattedPct() { return String.format(java.util.Locale.US, "%.1f%%", pct); }
    }

    public static class RecentActionRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String notifType, studentName, courseCode, sentDate, subject;
        public RecentActionRow(String t, String s, String c, String d, String sub) {
            this.notifType = t; this.studentName = s; this.courseCode = c;
            this.sentDate = d; this.subject = sub;
        }
        public String getNotifType() { return notifType; }
        public String getStudentName() { return studentName; }
        public String getCourseCode() { return courseCode; }
        public String getSentDate() { return sentDate; }
        public String getSubject() { return subject; }
        public String getColor() {
            switch (notifType) {
                case "EXCLUSION": return "#dc2626";
                case "CONVOCATION": return "#f59e0b";
                case "PARENT_ALERT": return "#3b82f6";
                default: return "#94a3b8";
            }
        }
    }
}