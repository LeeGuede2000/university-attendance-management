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

@Named("adminDashboardBean")
@SessionScoped
public class AdminDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final StatisticsService statsInstance = new StatisticsService();

    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_2";

    private int totalStudents, totalFaculties, totalCourses;
    private int totalSessions, totalAbsences, totalJustifications;

    private int nbStudentsTerm, nbFemaleStudents, nbMaleStudents;
    private int presencePct, femalePresencePct, malePresencePct;
    private int earlyPresencePct, noonPresencePct;
    
    private int nbExclusions;
    private int nbConvocations;
    private int nbParentAlerts;

    private List<StatisticsService.StudentAbsenceStat> topAbsent;
    private List<StatisticsService.MonthStat> monthStats;
    private List<StatisticsService.WeekStat> weekStats;
    private List<StatisticsService.FacultyStat> topFaculties;
    private List<StatisticsService.StudentAlertStat> alertStudents;
    private StatisticsService.ActionsTakenStat actionsStats;
    private List<CoursePresenceStat> courseStats = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadAll();
    }

    private void loadAll() {
        loadOverview();
        loadStatsPerTermPeriod();
        loadRichStats();
        loadCourseStats();
    }

    private void loadOverview() {
        EntityManager em = DataManager.getEntityManager();
        try {
            totalStudents = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM student").getSingleResult()).intValue();
            totalFaculties = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM faculty").getSingleResult()).intValue();
            totalCourses = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM course").getSingleResult()).intValue();
            totalSessions = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM attendance").getSingleResult()).intValue();
            totalAbsences = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM studentattendance WHERE isPresent = 0").getSingleResult()).intValue();
            totalJustifications = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM absencejustification").getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    private void loadStatsPerTermPeriod() {
        nbStudentsTerm = StatisticsService.countStudentsPerTermPeriod(null, selectedTermID, selectedPeriodID);
        nbFemaleStudents = StatisticsService.countFemaleStudentsPerTermPeriod(null, selectedTermID, selectedPeriodID);
        nbMaleStudents = StatisticsService.countMaleStudentsPerTermPeriod(null, selectedTermID, selectedPeriodID);
        presencePct = StatisticsService.presencePercentagePerTermPeriod(selectedTermID, selectedPeriodID);
        femalePresencePct = StatisticsService.femalePresencePercentagePerTermPeriod(selectedTermID, selectedPeriodID);
        malePresencePct = StatisticsService.malePresencePercentagePerTermPeriod(selectedTermID, selectedPeriodID);
        earlyPresencePct = StatisticsService.earlyPresencePercentagePerTermPeriod(selectedTermID, selectedPeriodID);
        noonPresencePct = StatisticsService.noonPresencePercentagePerTermPeriod(selectedTermID, selectedPeriodID);
    }

    private void loadRichStats() {
        topAbsent = statsInstance.getMostAbsentStudents(selectedTermID, selectedPeriodID);
        monthStats = statsInstance.getAbsencesByMonth(selectedTermID, selectedPeriodID);
        weekStats = statsInstance.getAbsencesByWeek(selectedTermID, selectedPeriodID);
        topFaculties = statsInstance.getTopAttendingFaculties(selectedTermID, selectedPeriodID);
        alertStudents = statsInstance.getStudentsWithAbsenceAlerts(selectedTermID, selectedPeriodID);
        actionsStats = statsInstance.getActionsTakenStats(selectedTermID, selectedPeriodID);
        
        nbExclusions = 0; nbConvocations = 0; nbParentAlerts = 0;
for (StatisticsService.StudentAlertStat a : alertStudents) {
    switch (a.getAlertLevel()) {
        case "EXCLUSION": nbExclusions++; break;
        case "CONVOCATION": nbConvocations++; break;
        case "PARENT_ALERT": nbParentAlerts++; break;
    }
}
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
                    + "GROUP BY c.code, c.name, c.school "
                    + "ORDER BY c.school, c.code")
                    .setParameter(1, selectedTermID)
                    .setParameter(2, selectedPeriodID)
                    .getResultList();

            for (Object[] r : rows) {
                int tot = ((Number) r[3]).intValue();
                int pre = ((Number) r[4]).intValue();
                int abs = tot - pre;
                double rate = tot > 0 ? (100.0 * pre / tot) : 0;
                courseStats.add(new CoursePresenceStat(
                        (String) r[0], (String) r[1], (String) r[2],
                        tot, pre, abs, rate));
            }
        } finally { em.close(); }
    }

    public String refresh() {
        loadAll();
        return null;
    }

    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) {
        this.selectedTermID = s;
        loadStatsPerTermPeriod();
        loadRichStats();
        loadCourseStats();
    }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) {
        this.selectedPeriodID = s;
        loadStatsPerTermPeriod();
        loadRichStats();
        loadCourseStats();
    }
    public int getTotalStudents() { return totalStudents; }
    public int getTotalFaculties() { return totalFaculties; }
    public int getTotalCourses() { return totalCourses; }
    public int getTotalSessions() { return totalSessions; }
    public int getTotalAbsences() { return totalAbsences; }
    public int getTotalJustifications() { return totalJustifications; }
    public int getNbStudentsTerm() { return nbStudentsTerm; }
    public int getNbFemaleStudents() { return nbFemaleStudents; }
    public int getNbMaleStudents() { return nbMaleStudents; }
    public int getPresencePct() { return presencePct; }
    public int getFemalePresencePct() { return femalePresencePct; }
    public int getMalePresencePct() { return malePresencePct; }
    public int getEarlyPresencePct() { return earlyPresencePct; }
    public int getNoonPresencePct() { return noonPresencePct; }
    public List<StatisticsService.StudentAbsenceStat> getTopAbsent() { return topAbsent; }
    public List<StatisticsService.MonthStat> getMonthStats() { return monthStats; }
    public List<StatisticsService.WeekStat> getWeekStats() { return weekStats; }
    public List<StatisticsService.FacultyStat> getTopFaculties() { return topFaculties; }
    public List<StatisticsService.StudentAlertStat> getAlertStudents() { return alertStudents; }
    public StatisticsService.ActionsTakenStat getActionsStats() { return actionsStats; }
    public List<CoursePresenceStat> getCourseStats() { return courseStats; }

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
    public int getNbExclusions() { return nbExclusions; }
public int getNbConvocations() { return nbConvocations; }
public int getNbParentAlerts() { return nbParentAlerts; }
}


