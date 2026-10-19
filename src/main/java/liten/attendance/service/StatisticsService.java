package liten.attendance.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import liten.attendance.util.DataManager;

public class StatisticsService {

    // ====================================================================
    // 8 MÉTHODES OBLIGATOIRES — SIGNATURES EXACTES DU PROF
    // ====================================================================

    public static int countStudentsPerTermPeriod(String studentID, String termID, String periodID) {
        return countStudentsHelper(studentID, termID, periodID, null);
    }

    public static int countFemaleStudentsPerTermPeriod(String studentID, String termID, String periodID) {
        return countStudentsHelper(studentID, termID, periodID, "F");
    }

    public static int countMaleStudentsPerTermPeriod(String studentID, String termID, String periodID) {
        return countStudentsHelper(studentID, termID, periodID, "M");
    }

    public static int presencePercentagePerTermPeriod(String termID, String periodID) {
        return computePresencePercentage(termID, periodID, null, null);
    }

    public static int femalePresencePercentagePerTermPeriod(String termID, String periodID) {
        return computePresencePercentage(termID, periodID, "F", null);
    }

    public static int malePresencePercentagePerTermPeriod(String termID, String periodID) {
        return computePresencePercentage(termID, periodID, "M", null);
    }

    public static int earlyPresencePercentagePerTermPeriod(String termID, String periodID) {
        return computePresencePercentage(termID, periodID, null, "EARLY");
    }

    public static int noonPresencePercentagePerTermPeriod(String termID, String periodID) {
        return computePresencePercentage(termID, periodID, null, "NOON");
    }

    // ====================================================================
    // HELPERS PRIVÉS
    // ====================================================================

    private static int countStudentsHelper(String studentID, String termID, String periodID, String genre) {
        EntityManager em = DataManager.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(DISTINCT sa.studentID) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate ");

            if (studentID != null && !studentID.isBlank()) sql.append("AND sa.studentID = ? ");
            if (genre != null) sql.append("AND s.genre = ? ");

            Query q = em.createNativeQuery(sql.toString());
            q.setParameter(1, termID);
            q.setParameter(2, periodID);
            int idx = 3;
            if (studentID != null && !studentID.isBlank()) q.setParameter(idx++, studentID);
            if (genre != null) q.setParameter(idx, genre);

            Number n = (Number) q.getSingleResult();
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    private static int computePresencePercentage(String termID, String periodID, String genre, String timeCondition) {
        EntityManager em = DataManager.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END), COUNT(*) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate ");

            if (genre != null) sql.append("AND s.genre = ? ");
            if ("EARLY".equals(timeCondition)) sql.append("AND a.startHour < '12:00:00' ");
            else if ("NOON".equals(timeCondition)) sql.append("AND a.startHour >= '12:00:00' ");

            Query q = em.createNativeQuery(sql.toString());
            q.setParameter(1, termID);
            q.setParameter(2, periodID);
            if (genre != null) q.setParameter(3, genre);

            Object[] result = (Object[]) q.getSingleResult();
            Number presents = (Number) result[0];
            Number total = (Number) result[1];
            if (total == null || total.intValue() == 0) return 0;
            return (int) Math.round(100.0 * presents.intValue() / total.intValue());
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }

    // ====================================================================
    // MÉTHODES COMPLÉMENTAIRES (dashboards) - filtrées par term+period
    // ====================================================================

    public List<StudentAbsenceStat> getMostAbsentStudents(String termID, String periodID) {
        EntityManager em = DataManager.getEntityManager();
        List<StudentAbsenceStat> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.studentID, s.firstName, s.lastName, s.matricule, s.school, COUNT(*) "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.isPresent = 0 AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.studentID, s.firstName, s.lastName, s.matricule, s.school "
                    + "ORDER BY COUNT(*) DESC LIMIT 10")
                    .setParameter(1, termID).setParameter(2, periodID).getResultList();
            for (Object[] r : rows) {
                result.add(new StudentAbsenceStat((String) r[0], (String) r[1], (String) r[2],
                        (String) r[3], (String) r[4], ((Number) r[5]).intValue()));
            }
        } finally { em.close(); }
        return result;
    }

    public List<MonthStat> getAbsencesByMonth(String termID, String periodID) {
        EntityManager em = DataManager.getEntityManager();
        List<MonthStat> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT DATE_FORMAT(a.attendanceDate, '%Y-%m'), COUNT(*) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.isPresent = 0 AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY DATE_FORMAT(a.attendanceDate, '%Y-%m') ORDER BY 1")
                    .setParameter(1, termID).setParameter(2, periodID).getResultList();
            for (Object[] r : rows) {
                result.add(new MonthStat((String) r[0], ((Number) r[1]).intValue()));
            }
        } finally { em.close(); }
        return result;
    }

    public List<WeekStat> getAbsencesByWeek(String termID, String periodID) {
        EntityManager em = DataManager.getEntityManager();
        List<WeekStat> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT YEARWEEK(a.attendanceDate, 1), MIN(a.attendanceDate), COUNT(*) "
                    + "FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.isPresent = 0 AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY YEARWEEK(a.attendanceDate, 1) ORDER BY 1")
                    .setParameter(1, termID).setParameter(2, periodID).getResultList();
            int weekNum = 1;
            for (Object[] r : rows) {
                String d = r[1] != null ? r[1].toString() : "";
                result.add(new WeekStat("S" + weekNum, d, ((Number) r[2]).intValue()));
                weekNum++;
            }
        } finally { em.close(); }
        return result;
    }

    public List<FacultyStat> getTopAttendingFaculties(String termID, String periodID) {
        EntityManager em = DataManager.getEntityManager();
        List<FacultyStat> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT f.facultyID, f.firstName, f.lastName, f.departmentName, "
                    + "  COUNT(sa.studentID), SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END) "
                    + "FROM attendance a "
                    + "JOIN faculty f ON f.facultyID = a.facultyID "
                    + "JOIN studentattendance sa ON sa.attendanceID = a.attendanceID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY f.facultyID, f.firstName, f.lastName, f.departmentName "
                    + "HAVING COUNT(sa.studentID) > 0 "
                    + "ORDER BY (SUM(CASE WHEN sa.isPresent=1 THEN 1 ELSE 0 END)/COUNT(sa.studentID)) DESC LIMIT 10")
                    .setParameter(1, termID).setParameter(2, periodID).getResultList();
            for (Object[] r : rows) {
                int tot = ((Number) r[4]).intValue();
                int pre = ((Number) r[5]).intValue();
                double pct = tot > 0 ? (100.0 * pre / tot) : 0;
                result.add(new FacultyStat((String) r[0], (String) r[1], (String) r[2],
                        (String) r[3], tot, pre, pct));
            }
        } finally { em.close(); }
        return result;
    }

    public List<StudentAlertStat> getStudentsWithAbsenceAlerts(String termID, String periodID) {
        EntityManager em = DataManager.getEntityManager();
        List<StudentAlertStat> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.studentID, s.firstName, s.lastName, s.matricule, s.school, c.code, COUNT(*) "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "JOIN course c ON c.courseID = a.courseID "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE sa.isPresent = 0 AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                    + "GROUP BY s.studentID, s.firstName, s.lastName, s.matricule, s.school, c.code "
                    + "HAVING COUNT(*) >= 3 ORDER BY COUNT(*) DESC")
                    .setParameter(1, termID).setParameter(2, periodID).getResultList();
            for (Object[] r : rows) {
                int nb = ((Number) r[6]).intValue();
                String lvl = nb >= 5 ? "EXCLUSION" : (nb == 4 ? "CONVOCATION" : "PARENT_ALERT");
                String col = nb >= 5 ? "#dc2626" : (nb == 4 ? "#f59e0b" : "#3b82f6");
                result.add(new StudentAlertStat((String) r[0], (String) r[1], (String) r[2],
                        (String) r[3], (String) r[4], (String) r[5], nb, lvl, col));
            }
        } finally { em.close(); }
        return result;
    }

    public ActionsTakenStat getActionsTakenStats(String termID, String periodID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            Object[] totals = (Object[]) em.createNativeQuery(
                    "SELECT COUNT(*), "
                    + "  SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END), "
                    + "  SUM(CASE WHEN isCompleted = 0 THEN 1 ELSE 0 END) "
                    + "FROM action act "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE act.actionDate BETWEEN tp.startDate AND tp.endDate")
                    .setParameter(1, termID).setParameter(2, periodID).getSingleResult();

            int total = totals[0] != null ? ((Number) totals[0]).intValue() : 0;
            int completed = totals[1] != null ? ((Number) totals[1]).intValue() : 0;
            int pending = totals[2] != null ? ((Number) totals[2]).intValue() : 0;

            @SuppressWarnings("unchecked")
            List<Object[]> levelRows = em.createNativeQuery(
                    "SELECT CASE "
                    + "  WHEN nbrAbsences >= 5 THEN 'EXCLUSION' "
                    + "  WHEN nbrAbsences = 4 THEN 'CONVOCATION' "
                    + "  WHEN nbrAbsences = 3 THEN 'PARENT_ALERT' "
                    + "  ELSE 'AUTRE' END, COUNT(*) "
                    + "FROM action act "
                    + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                    + "WHERE act.actionDate BETWEEN tp.startDate AND tp.endDate GROUP BY 1")
                    .setParameter(1, termID).setParameter(2, periodID).getResultList();

            int nE = 0, nC = 0, nP = 0, nA = 0;
            for (Object[] r : levelRows) {
                String niv = (String) r[0];
                int cnt = ((Number) r[1]).intValue();
                switch (niv) {
                    case "EXCLUSION": nE = cnt; break;
                    case "CONVOCATION": nC = cnt; break;
                    case "PARENT_ALERT": nP = cnt; break;
                    default: nA = cnt;
                }
            }
            return new ActionsTakenStat(total, completed, pending, nE, nC, nP, nA);
        } catch (Exception e) {
            return new ActionsTakenStat(0, 0, 0, 0, 0, 0, 0);
        } finally {
            em.close();
        }
    }

    // ====================================================================
    // DTOs
    // ====================================================================

    public static class StudentAbsenceStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, firstName, lastName, matricule, school;
        private final int nbAbsences;
        public StudentAbsenceStat(String id, String fn, String ln, String m, String s, int n) {
            this.studentID = id; this.firstName = fn; this.lastName = ln;
            this.matricule = m; this.school = s; this.nbAbsences = n;
        }
        public String getStudentID() { return studentID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getMatricule() { return matricule; }
        public String getSchool() { return school; }
        public int getNbAbsences() { return nbAbsences; }
    }

    public static class MonthStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String yearMonth;
        private final int nbAbsences;
        public MonthStat(String ym, int nb) { this.yearMonth = ym; this.nbAbsences = nb; }
        public String getYearMonth() { return yearMonth; }
        public int getNbAbsences() { return nbAbsences; }
    }

    public static class WeekStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label, startDate;
        private final int nbAbsences;
        public WeekStat(String l, String d, int n) {
            this.label = l; this.startDate = d; this.nbAbsences = n;
        }
        public String getLabel() { return label; }
        public String getStartDate() { return startDate; }
        public int getNbAbsences() { return nbAbsences; }
    }

    public static class FacultyStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String facultyID, firstName, lastName, department;
        private final int total, presents;
        private final double percentage;
        public FacultyStat(String id, String fn, String ln, String d, int t, int p, double pct) {
            this.facultyID = id; this.firstName = fn; this.lastName = ln;
            this.department = d; this.total = t; this.presents = p; this.percentage = pct;
        }
        public String getFacultyID() { return facultyID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getDepartment() { return department; }
        public int getTotal() { return total; }
        public int getPresents() { return presents; }
        public double getPercentage() { return percentage; }
        public String getFormattedPercentage() {
            return String.format(Locale.US, "%.1f%%", percentage);
        }
    }

    public static class StudentAlertStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, firstName, lastName, matricule, school, courseCode;
        private final int nbAbsences;
        private final String alertLevel, alertColor;
        public StudentAlertStat(String id, String fn, String ln, String m, String s,
                                String cc, int n, String l, String col) {
            this.studentID = id; this.firstName = fn; this.lastName = ln;
            this.matricule = m; this.school = s; this.courseCode = cc;
            this.nbAbsences = n; this.alertLevel = l; this.alertColor = col;
        }
        public String getStudentID() { return studentID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getMatricule() { return matricule; }
        public String getSchool() { return school; }
        public String getCourseCode() { return courseCode; }
        public int getNbAbsences() { return nbAbsences; }
        public String getAlertLevel() { return alertLevel; }
        public String getAlertColor() { return alertColor; }
    }

    public static class ActionsTakenStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int total, completed, pending;
        private final int forExclusion, forConvocation, forParentAlert, autre;
        public ActionsTakenStat(int t, int c, int p, int e, int cv, int pa, int au) {
            this.total = t; this.completed = c; this.pending = p;
            this.forExclusion = e; this.forConvocation = cv;
            this.forParentAlert = pa; this.autre = au;
        }
        public int getTotal() { return total; }
        public int getCompleted() { return completed; }
        public int getPending() { return pending; }
        public int getForExclusion() { return forExclusion; }
        public int getForConvocation() { return forConvocation; }
        public int getForParentAlert() { return forParentAlert; }
        public int getAutre() { return autre; }
        public int getCompletionRate() {
            if (total == 0) return 0;
            return (int) Math.round(100.0 * completed / total);
        }
    }
}