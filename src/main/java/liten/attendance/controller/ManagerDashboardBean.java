package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.service.StatisticsService;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("managerDashboardBean")
@SessionScoped
public class ManagerDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final StatisticsService statsInstance = new StatisticsService();

    private String managerName;
    private String managerDirection;

    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_3";

    private int totalEmployees, totalRules, totalActions, totalStudents, totalFaculties;

    private List<SchoolStat> statsBySchool = new ArrayList<>();
    private List<LevelStat> statsByLevel = new ArrayList<>();
    private List<CauseStat> statsByCause = new ArrayList<>();
    private List<CoursePresenceStat> courseStats = new ArrayList<>();
    private List<FacultyPerformance> facultyPerformances = new ArrayList<>();
    private List<StudentPerformance> studentPerformances = new ArrayList<>();

    private int currentYearAbsences, lastYearAbsences;
    private List<StatisticsService.StudentAlertStat> alertStudents;
    private int nbExclusions, nbConvocations, nbParentAlerts;
    private List<StatisticsService.MonthStat> monthStats;

    @PostConstruct
    public void init() {
        resolveCurrent();
        loadAll();
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID == null) return;
        EntityManager em = DataManager.getEntityManager();
        try {
            Object[] row = (Object[]) em.createNativeQuery(
                    "SELECT firstName, lastName, direction FROM manager WHERE managerID = ?")
                    .setParameter(1, personID)
                    .getSingleResult();
            managerName = row[0] + " " + row[1];
            managerDirection = (String) row[2];
        } catch (Exception e) {
            managerName = "Manager"; managerDirection = "Direction";
        } finally { em.close(); }
    }

    private void loadAll() {
        loadCounters();
        loadSchoolStats();
        loadLevelStats();
        loadCauseStats();
        loadCourseStats();
        loadFacultyPerformances();
        loadStudentPerformances();
        loadYearlyComparison();
        loadAlerts();
        monthStats = statsInstance.getAbsencesByMonth(selectedTermID, selectedPeriodID);
    }

    private void loadCounters() {
        EntityManager em = DataManager.getEntityManager();
        try {
            totalEmployees = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM employee").getSingleResult()).intValue();
            totalStudents = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM student").getSingleResult()).intValue();
            totalFaculties = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM faculty").getSingleResult()).intValue();
            try { totalRules = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM rules").getSingleResult()).intValue(); }
            catch (Exception e) { totalRules = 0; }
            try { totalActions = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM action").getSingleResult()).intValue(); }
            catch (Exception e) { totalActions = 0; }
        } finally { em.close(); }
    }

    private void loadSchoolStats() {
        statsBySchool.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.school, "
                    + "  COUNT(sa.studentID) AS total, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 AND s.genre = 'F' THEN 1 ELSE 0 END) AS femalePresents, "
                    + "  SUM(CASE WHEN s.genre = 'F' THEN 1 ELSE 0 END) AS femaleTotal, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 AND s.genre = 'M' THEN 1 ELSE 0 END) AS malePresents, "
                    + "  SUM(CASE WHEN s.genre = 'M' THEN 1 ELSE 0 END) AS maleTotal "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE s.school IS NOT NULL "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.school ORDER BY s.school")
                    .setParameter(1, selectedTermID).setParameter(2, selectedPeriodID)
                    .getResultList();
            for (Object[] r : rows) {
                int total = ((Number) r[1]).intValue();
                int presents = ((Number) r[2]).intValue();
                int fp = r[3] != null ? ((Number) r[3]).intValue() : 0;
                int ft = r[4] != null ? ((Number) r[4]).intValue() : 0;
                int mp = r[5] != null ? ((Number) r[5]).intValue() : 0;
                int mt = r[6] != null ? ((Number) r[6]).intValue() : 0;
                double pct = total > 0 ? (100.0 * presents / total) : 0;
                double fpct = ft > 0 ? (100.0 * fp / ft) : 0;
                double mpct = mt > 0 ? (100.0 * mp / mt) : 0;
                statsBySchool.add(new SchoolStat((String) r[0], total, presents, pct, fpct, mpct));
            }
        } finally { em.close(); }
    }

    private void loadLevelStats() {
        statsByLevel.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.academicLevel, "
                    + "  COUNT(sa.studentID) AS total, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents, "
                    + "  SUM(CASE WHEN sa.isPresent = 0 THEN 1 ELSE 0 END) AS absences "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE s.academicLevel IS NOT NULL "
                    + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.academicLevel ORDER BY s.academicLevel")
                    .setParameter(1, selectedTermID).setParameter(2, selectedPeriodID)
                    .getResultList();
            for (Object[] r : rows) {
                int total = ((Number) r[1]).intValue();
                int presents = ((Number) r[2]).intValue();
                int absences = ((Number) r[3]).intValue();
                double pct = total > 0 ? (100.0 * presents / total) : 0;
                statsByLevel.add(new LevelStat((String) r[0], total, presents, absences, pct));
            }
        } finally { em.close(); }
    }

  private void loadCauseStats() {
    statsByCause.clear();
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ct.name, ct.description, ct.causeCategory, COUNT(*) AS nb "
                + "FROM absencejustification aj "
                + "JOIN causetype ct ON ct.causeTypeID = aj.causeTypeID "
                + "GROUP BY ct.name, ct.description, ct.causeCategory "
                + "ORDER BY ct.causeCategory, nb DESC")
                .getResultList();
        for (Object[] r : rows) {
            statsByCause.add(new CauseStat(
                    (String) r[0], (String) r[1],
                    r[2] != null ? (String) r[2] : "—",
                    ((Number) r[3]).intValue()));
        }
    } catch (Exception e) { } finally { em.close(); }
}

    private void loadCourseStats() {
        courseStats.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT c.code, c.name, c.school, "
                    + "  COUNT(sa.studentID) AS totalSessions, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents "
                    + "FROM course c "
                    + "JOIN attendance a ON a.courseID = c.courseID "
                    + "JOIN studentattendance sa ON sa.attendanceID = a.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY c.code, c.name, c.school ORDER BY c.school, c.code")
                    .setParameter(1, selectedTermID).setParameter(2, selectedPeriodID)
                    .getResultList();
            for (Object[] r : rows) {
                int tot = ((Number) r[3]).intValue();
                int pre = ((Number) r[4]).intValue();
                int abs = tot - pre;
                double rate = tot > 0 ? (100.0 * pre / tot) : 0;
                courseStats.add(new CoursePresenceStat(
                        (String) r[0], (String) r[1], (String) r[2], tot, pre, abs, rate));
            }
        } finally { em.close(); }
    }

    private void loadFacultyPerformances() {
        facultyPerformances.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT f.facultyID, f.firstName, f.lastName, f.departmentName, "
                    + "  COUNT(sa.studentID) AS totalSessions, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents, "
                    + "  COUNT(DISTINCT a.courseID) AS nbCourses "
                    + "FROM faculty f "
                    + "JOIN attendance a ON a.facultyID = f.facultyID "
                    + "JOIN studentattendance sa ON sa.attendanceID = a.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY f.facultyID, f.firstName, f.lastName, f.departmentName "
                    + "ORDER BY presents DESC")
                    .setParameter(1, selectedTermID).setParameter(2, selectedPeriodID)
                    .getResultList();
            for (Object[] r : rows) {
                int tot = ((Number) r[4]).intValue();
                int pre = ((Number) r[5]).intValue();
                int nbCourses = ((Number) r[6]).intValue();
                double rate = tot > 0 ? (100.0 * pre / tot) : 0;
                facultyPerformances.add(new FacultyPerformance(
                        (String) r[0], r[1] + " " + r[2],
                        (String) r[3], tot, pre, tot - pre, rate, nbCourses));
            }
        } finally { em.close(); }
    }

    private void loadStudentPerformances() {
        studentPerformances.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.studentID, s.firstName, s.lastName, s.matricule, s.school, "
                    + "  s.academicLevel, "
                    + "  COUNT(sa.attendanceID) AS totalSessions, "
                    + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents "
                    + "FROM student s "
                    + "JOIN studentattendance sa ON sa.studentID = s.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.studentID, s.firstName, s.lastName, s.matricule, s.school, s.academicLevel "
                    + "ORDER BY presents ASC LIMIT 50")
                    .setParameter(1, selectedTermID).setParameter(2, selectedPeriodID)
                    .getResultList();
            for (Object[] r : rows) {
                int tot = ((Number) r[6]).intValue();
                int pre = ((Number) r[7]).intValue();
                double rate = tot > 0 ? (100.0 * pre / tot) : 0;
                studentPerformances.add(new StudentPerformance(
                        (String) r[0], r[1] + " " + r[2],
                        (String) r[3], (String) r[4], (String) r[5],
                        tot, pre, tot - pre, rate));
            }
        } finally { em.close(); }
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
                    .setParameter(1, periodID).getSingleResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) { return 0; }
        finally { em.close(); }
    }

    private void loadAlerts() {
        alertStudents = statsInstance.getStudentsWithAbsenceAlerts(selectedTermID, selectedPeriodID);
        nbExclusions = 0; nbConvocations = 0; nbParentAlerts = 0;
        for (StatisticsService.StudentAlertStat a : alertStudents) {
            switch (a.getAlertLevel()) {
                case "EXCLUSION": nbExclusions++; break;
                case "CONVOCATION": nbConvocations++; break;
                case "PARENT_ALERT": nbParentAlerts++; break;
            }
        }
    }

    public String refresh() { loadAll(); return null; }

    public int getEvolutionPct() {
        if (lastYearAbsences == 0) return 0;
        return (int) Math.round(100.0 * (currentYearAbsences - lastYearAbsences) / lastYearAbsences);
    }

    public String getManagerName() { return managerName; }
    public String getManagerDirection() { return managerDirection; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; loadAll(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; loadAll(); }
    public int getTotalEmployees() { return totalEmployees; }
    public int getTotalRules() { return totalRules; }
    public int getTotalActions() { return totalActions; }
    public int getTotalStudents() { return totalStudents; }
    public int getTotalFaculties() { return totalFaculties; }
    public List<SchoolStat> getStatsBySchool() { return statsBySchool; }
    public List<LevelStat> getStatsByLevel() { return statsByLevel; }
    public List<CauseStat> getStatsByCause() { return statsByCause; }
    public List<CoursePresenceStat> getCourseStats() { return courseStats; }
    public List<FacultyPerformance> getFacultyPerformances() { return facultyPerformances; }
    public List<StudentPerformance> getStudentPerformances() { return studentPerformances; }
    public int getCurrentYearAbsences() { return currentYearAbsences; }
    public int getLastYearAbsences() { return lastYearAbsences; }
    public List<StatisticsService.StudentAlertStat> getAlertStudents() { return alertStudents; }
    public int getNbExclusions() { return nbExclusions; }
    public int getNbConvocations() { return nbConvocations; }
    public int getNbParentAlerts() { return nbParentAlerts; }
    public List<StatisticsService.MonthStat> getMonthStats() { return monthStats; }

    // ==================== INNER CLASSES ====================

    public static class SchoolStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String school;
        private final int total, presents;
        private final double pct, femalePct, malePct;
        public SchoolStat(String s, int t, int p, double pc, double fp, double mp) {
            this.school = s; this.total = t; this.presents = p;
            this.pct = pc; this.femalePct = fp; this.malePct = mp;
        }
        public String getSchool() { return school; }
        public int getTotal() { return total; }
        public int getPresents() { return presents; }
        public double getPct() { return pct; }
        public String getFormattedPct() { return String.format(java.util.Locale.US, "%.1f%%", pct); }
        public String getFormattedFemalePct() { return String.format(java.util.Locale.US, "%.1f%%", femalePct); }
        public String getFormattedMalePct() { return String.format(java.util.Locale.US, "%.1f%%", malePct); }
    }

    public static class LevelStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String level;
        private final int total, presents, absences;
        private final double pct;
        public LevelStat(String l, int t, int p, int a, double pc) {
            this.level = l; this.total = t; this.presents = p; this.absences = a; this.pct = pc;
        }
        public String getLevel() { return level; }
        public int getTotal() { return total; }
        public int getPresents() { return presents; }
        public int getAbsences() { return absences; }
        public double getPct() { return pct; }
        public String getFormattedPct() { return String.format(java.util.Locale.US, "%.1f%%", pct); }
    }

 public static class CauseStat implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String causeName, description, category;
    private final int nbJustifications;
    public CauseStat(String n, String d, String cat, int nb) {
        this.causeName = n; this.description = d;
        this.category = cat; this.nbJustifications = nb;
    }
    public String getCauseName() { return causeName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getNbJustifications() { return nbJustifications; }
    public String getCategoryColor() {
    switch (category != null ? category : "") {
        case "Médical":        return "#ec4899";
        case "Familial":       return "#8b5cf6";
        case "Logistique":     return "#f59e0b";
        case "Personnelle":    return "#64748b";
        case "Administratif":  return "#3b82f6";
        case "Religieux":      return "#0891b2";
        case "Événementiel":   return "#16a34a";
        case "Sanctionnel":    return "#dc2626";
        case "Autre":          return "#94a3b8";
        default:               return "#94a3b8";
    }
}
}

    public static class CoursePresenceStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String code, name, school;
        private final int totalSessions, totalPresents, totalAbsences;
        private final double presenceRate;
        public CoursePresenceStat(String c, String n, String s, int ts, int tp, int ta, double pr) {
            this.code = c; this.name = n; this.school = s;
            this.totalSessions = ts; this.totalPresents = tp;
            this.totalAbsences = ta; this.presenceRate = pr;
        }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSchool() { return school; }
        public int getTotalSessions() { return totalSessions; }
        public int getTotalPresents() { return totalPresents; }
        public int getTotalAbsences() { return totalAbsences; }
        public double getPresenceRate() { return presenceRate; }
        public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", presenceRate); }
        public String getRateColor() {
            if (presenceRate >= 85) return "#16a34a";
            if (presenceRate >= 70) return "#3b82f6";
            if (presenceRate >= 55) return "#f59e0b";
            return "#dc2626";
        }
    }

    public static class FacultyPerformance implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String facultyID, fullName, department;
        private final int totalSessions, totalPresents, totalAbsences, nbCourses;
        private final double presenceRate;
        public FacultyPerformance(String id, String fn, String dep, int ts, int tp, int ta, double pr, int nc) {
            this.facultyID = id; this.fullName = fn; this.department = dep;
            this.totalSessions = ts; this.totalPresents = tp;
            this.totalAbsences = ta; this.presenceRate = pr; this.nbCourses = nc;
        }
        public String getFacultyID() { return facultyID; }
        public String getFullName() { return fullName; }
        public String getDepartment() { return department; }
        public int getTotalSessions() { return totalSessions; }
        public int getTotalPresents() { return totalPresents; }
        public int getTotalAbsences() { return totalAbsences; }
        public int getNbCourses() { return nbCourses; }
        public double getPresenceRate() { return presenceRate; }
        public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", presenceRate); }
        public String getRateColor() {
            if (presenceRate >= 85) return "#16a34a";
            if (presenceRate >= 70) return "#3b82f6";
            if (presenceRate >= 55) return "#f59e0b";
            return "#dc2626";
        }
    }

    public static class StudentPerformance implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, fullName, matricule, school, level;
        private final int totalSessions, totalPresents, totalAbsences;
        private final double presenceRate;
        public StudentPerformance(String id, String fn, String m, String sc, String lv,
                                   int ts, int tp, int ta, double pr) {
            this.studentID = id; this.fullName = fn; this.matricule = m;
            this.school = sc; this.level = lv;
            this.totalSessions = ts; this.totalPresents = tp;
            this.totalAbsences = ta; this.presenceRate = pr;
        }
        public String getStudentID() { return studentID; }
        public String getFullName() { return fullName; }
        public String getMatricule() { return matricule; }
        public String getSchool() { return school; }
        public String getLevel() { return level; }
        public int getTotalSessions() { return totalSessions; }
        public int getTotalPresents() { return totalPresents; }
        public int getTotalAbsences() { return totalAbsences; }
        public double getPresenceRate() { return presenceRate; }
        public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", presenceRate); }
        public String getRateColor() {
            if (presenceRate >= 85) return "#16a34a";
            if (presenceRate >= 70) return "#3b82f6";
            if (presenceRate >= 55) return "#f59e0b";
            return "#dc2626";
        }
        public String getAlertLevel() {
            if (totalAbsences >= 5) return "EXCLUSION";
            if (totalAbsences == 4) return "CONVOCATION";
            if (totalAbsences == 3) return "PARENT_ALERT";
            return "";
        }
        public String getAlertColor() {
            if (totalAbsences >= 5) return "#dc2626";
            if (totalAbsences == 4) return "#f59e0b";
            if (totalAbsences == 3) return "#3b82f6";
            return "#16a34a";
        }
    }
}