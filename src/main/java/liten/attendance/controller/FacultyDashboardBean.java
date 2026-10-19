package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.dao.CourseDAO;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.dao.JustificationDAO;
import liten.attendance.domain.Course;
import liten.attendance.domain.Faculty;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("facultyDashboardBean")
@SessionScoped
public class FacultyDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final JustificationDAO justificationDAO = new JustificationDAO();

    private Faculty currentFaculty;
    private List<Course> myCourses = new ArrayList<>();

    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_3";

    // Compteurs
    private int nbSessionsTermPeriod;
    private int nbPendingDecisions;
    private int nbPendingAdvisorReviews;

    // Stats école
    private int schoolPresencePct;
    private int schoolFemalePct;
    private int schoolMalePct;

    // Stats par cours
    private List<CourseStat> courseStats = new ArrayList<>();

    // Comparaison year-over-year
    private int currentYearAbsences;
    private int lastYearAbsences;

    // Étudiants à risque dans mes cours
    private List<StudentAtRisk> studentsAtRisk = new ArrayList<>();

    @PostConstruct
    public void init() {
        resolveCurrent();
        if (currentFaculty != null) {
            loadAll();
        }
    }

  private void loadAll() {
    loadMyCoursesReal();
    loadCounters();
    loadSchoolStats();
    loadCourseStats();
    loadYearComparison();
    loadStudentsAtRisk();
}

private void loadMyCoursesReal() {
    myCourses.clear();
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT DISTINCT c.courseID, c.code, c.name, c.school "
                + "FROM attendance a JOIN course c ON c.courseID = a.courseID "
                + "WHERE a.facultyID = ? "
                + "ORDER BY c.code")
                .setParameter(1, currentFaculty.getFacultyID())
                .getResultList();
        for (Object[] r : rows) {
            Course c = new Course();
            c.setCourseID((String) r[0]);
            c.setCode((String) r[1]);
            c.setName((String) r[2]);
            c.setSchool((String) r[3]);
            myCourses.add(c);
        }
    } finally {
        em.close();
    }
}

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID == null) return;
        currentFaculty = facultyDAO.findById(personID);
    }

    private void loadCounters() {
        EntityManager em = DataManager.getEntityManager();
        try {
            Number n = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM attendance a "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.facultyID = ? "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, currentFaculty.getFacultyID())
                    .getSingleResult();
            nbSessionsTermPeriod = n != null ? n.intValue() : 0;
        } catch (Exception e) {
            nbSessionsTermPeriod = 0;
        } finally {
            em.close();
        }

        try {
            nbPendingDecisions = justificationDAO.findPendingForFaculty(currentFaculty.getFacultyID()).size();
        } catch (Exception e) { nbPendingDecisions = 0; }

        if (Boolean.TRUE.equals(currentFaculty.getIsAdvisor())) {
            try {
                nbPendingAdvisorReviews = justificationDAO.findPendingForAdvisor(currentFaculty.getFacultyID()).size();
            } catch (Exception e) { nbPendingAdvisorReviews = 0; }
        }
    }

    private void loadSchoolStats() {
        EntityManager em = DataManager.getEntityManager();
        try {
            String school = currentFaculty.getDepartmentName();
            if (school == null) { schoolPresencePct = 0; schoolFemalePct = 0; schoolMalePct = 0; return; }

            schoolPresencePct = computePct(em, school, null);
            schoolFemalePct = computePct(em, school, "F");
            schoolMalePct = computePct(em, school, "M");
        } catch (Exception e) {
            schoolPresencePct = 0; schoolFemalePct = 0; schoolMalePct = 0;
        } finally {
            em.close();
        }
    }

    private int computePct(EntityManager em, String school, String genre) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END), COUNT(*) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE s.school = ? "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate ");
            if (genre != null) sql.append("AND s.genre = ?");

            jakarta.persistence.Query q = em.createNativeQuery(sql.toString())
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, school);
            if (genre != null) q.setParameter(4, genre);

            Object[] r = (Object[]) q.getSingleResult();
            int p = r[0] != null ? ((Number) r[0]).intValue() : 0;
            int t = r[1] != null ? ((Number) r[1]).intValue() : 0;
            return t > 0 ? (int) Math.round(100.0 * p / t) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadCourseStats() {
        courseStats.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT c.courseID, c.code, c.name, "
                    + "  COUNT(DISTINCT a.attendanceID) AS sessions, "
                    + "  COUNT(sa.studentID) AS total, "
                    + "  SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END) AS presents "
                    + "FROM attendance a "
                    + "JOIN course c ON c.courseID = a.courseID "
                    + "LEFT JOIN studentattendance sa ON sa.attendanceID = a.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.facultyID = ? "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY c.courseID, c.code, c.name "
                    + "ORDER BY c.code")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, currentFaculty.getFacultyID())
                    .getResultList();

            for (Object[] r : rows) {
                int sessions = r[3] != null ? ((Number) r[3]).intValue() : 0;
                int total = r[4] != null ? ((Number) r[4]).intValue() : 0;
                int presents = r[5] != null ? ((Number) r[5]).intValue() : 0;
                int absences = total - presents;
                double pct = total > 0 ? (100.0 * presents / total) : 0;
                courseStats.add(new CourseStat(
                        (String) r[0], (String) r[1], (String) r[2],
                        sessions, presents, absences, pct));
            }
        } finally {
            em.close();
        }
    }

    private void loadYearComparison() {
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
                    + "WHERE a.facultyID = ? "
                    + "  AND sa.isPresent = 0 "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, periodID)
                    .setParameter(2, currentFaculty.getFacultyID())
                    .getSingleResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    private void loadStudentsAtRisk() {
        studentsAtRisk.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.studentID, s.firstName, s.lastName, s.matricule, "
                    + "  c.code, COUNT(*) AS nbAbs "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN course c ON c.courseID = a.courseID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.facultyID = ? "
                    + "  AND sa.isPresent = 0 "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.studentID, s.firstName, s.lastName, s.matricule, c.code "
                    + "HAVING COUNT(*) >= 3 "
                    + "ORDER BY nbAbs DESC")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .setParameter(3, currentFaculty.getFacultyID())
                    .getResultList();

            for (Object[] r : rows) {
                int nb = ((Number) r[5]).intValue();
                String level = nb >= 5 ? "EXCLUSION" : (nb == 4 ? "CONVOCATION" : "PARENT_ALERT");
                String color = nb >= 5 ? "#dc2626" : (nb == 4 ? "#f59e0b" : "#3b82f6");
                studentsAtRisk.add(new StudentAtRisk(
                        (String) r[0], (String) r[1], (String) r[2],
                        (String) r[3], (String) r[4], nb, level, color));
            }
        } finally {
            em.close();
        }
    }

    public String refresh() {
        if (currentFaculty != null) loadAll();
        return null;
    }

    public int getEvolutionPct() {
        if (lastYearAbsences == 0) return 0;
        return (int) Math.round(100.0 * (currentYearAbsences - lastYearAbsences) / lastYearAbsences);
    }

    // Getters
    public Faculty getCurrentFaculty() { return currentFaculty; }
    public List<Course> getMyCourses() { return myCourses; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; if (currentFaculty != null) loadAll(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; if (currentFaculty != null) loadAll(); }
    public int getNbSessionsTermPeriod() { return nbSessionsTermPeriod; }
    public int getNbPendingDecisions() { return nbPendingDecisions; }
    public int getNbPendingAdvisorReviews() { return nbPendingAdvisorReviews; }
    public int getSchoolPresencePct() { return schoolPresencePct; }
    public int getSchoolFemalePct() { return schoolFemalePct; }
    public int getSchoolMalePct() { return schoolMalePct; }
    public List<CourseStat> getCourseStats() { return courseStats; }
    public int getCurrentYearAbsences() { return currentYearAbsences; }
    public int getLastYearAbsences() { return lastYearAbsences; }
    public List<StudentAtRisk> getStudentsAtRisk() { return studentsAtRisk; }
    public boolean isAdvisor() {
        return currentFaculty != null && Boolean.TRUE.equals(currentFaculty.getIsAdvisor());
    }

    public static class CourseStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String courseID, code, name;
        private final int sessions, presents, absences;
        private final double pct;
        public CourseStat(String id, String c, String n, int s, int p, int a, double pct) {
            this.courseID = id; this.code = c; this.name = n;
            this.sessions = s; this.presents = p; this.absences = a; this.pct = pct;
        }
        public String getCourseID() { return courseID; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public int getSessions() { return sessions; }
        public int getPresents() { return presents; }
        public int getAbsences() { return absences; }
        public double getPct() { return pct; }
        public String getFormattedPct() { return String.format(java.util.Locale.US, "%.1f%%", pct); }
        public String getColor() {
            if (pct >= 85) return "#16a34a";
            if (pct >= 70) return "#3b82f6";
            if (pct >= 55) return "#f59e0b";
            return "#dc2626";
        }
    }

    public static class StudentAtRisk implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, firstName, lastName, matricule, courseCode;
        private final int nbAbsences;
        private final String alertLevel, alertColor;
        public StudentAtRisk(String id, String fn, String ln, String m, String cc, int nb, String l, String col) {
            this.studentID = id; this.firstName = fn; this.lastName = ln;
            this.matricule = m; this.courseCode = cc; this.nbAbsences = nb;
            this.alertLevel = l; this.alertColor = col;
        }
        public String getStudentID() { return studentID; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getMatricule() { return matricule; }
        public String getCourseCode() { return courseCode; }
        public int getNbAbsences() { return nbAbsences; }
        public String getAlertLevel() { return alertLevel; }
        public String getAlertColor() { return alertColor; }
    }
}