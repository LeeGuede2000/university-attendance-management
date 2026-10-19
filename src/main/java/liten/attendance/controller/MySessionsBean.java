package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.domain.Faculty;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("mySessionsBean")
@SessionScoped
public class MySessionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final FacultyDAO facultyDAO = new FacultyDAO();

    private Faculty currentFaculty;

    private String selectedTermID = "";
    private String selectedPeriodID = "";
    private String selectedCourseID = "";

    private List<CourseOption> myCourses = new ArrayList<>();
    private List<SessionRow> sessions = new ArrayList<>();
    private SessionDetail selectedDetail;

    @PostConstruct
    public void init() {
        resolveCurrent();
        if (currentFaculty != null) {
            loadCourses();
            loadSessions();
        }
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID != null) currentFaculty = facultyDAO.findById(personID);
    }

    private void loadCourses() {
        myCourses.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT DISTINCT c.courseID, c.code, c.name "
                    + "FROM attendance a JOIN course c ON c.courseID = a.courseID "
                    + "WHERE a.facultyID = ? ORDER BY c.code")
                    .setParameter(1, currentFaculty.getFacultyID())
                    .getResultList();
            for (Object[] r : rows) {
                myCourses.add(new CourseOption((String) r[0], (String) r[1], (String) r[2]));
            }
        } finally { em.close(); }
    }

    private void loadSessions() {
        sessions.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT a.attendanceID, c.code, c.name, a.attendanceDate, "
                    + "  a.startHour, a.endHour, a.totalEnrolled, a.totalPresents, "
                    + "  a.assistancePicture, a.assistancePicture2 "
                    + "FROM attendance a "
                    + "JOIN course c ON c.courseID = a.courseID "
                    + "WHERE a.facultyID = ? ");

            if (selectedCourseID != null && !selectedCourseID.isBlank()) {
                sql.append("AND a.courseID = ? ");
            }
            sql.append("ORDER BY a.attendanceDate DESC, a.startHour DESC LIMIT 50");

            jakarta.persistence.Query q = em.createNativeQuery(sql.toString())
                    .setParameter(1, currentFaculty.getFacultyID());
            if (selectedCourseID != null && !selectedCourseID.isBlank()) {
                q.setParameter(2, selectedCourseID);
            }

            @SuppressWarnings("unchecked")
            List<Object[]> rows = q.getResultList();

            for (Object[] r : rows) {
                String attID = (String) r[0];
                String date = r[3] != null ? r[3].toString().substring(0, 10) : "";
                String start = r[4] != null ? r[4].toString().substring(0, 5) : "";
                String end = r[5] != null ? r[5].toString().substring(0, 5) : "";
                int enrolled = r[6] != null ? ((Number) r[6]).intValue() : 0;
                int presents = r[7] != null ? ((Number) r[7]).intValue() : 0;
                double rate = enrolled > 0 ? (100.0 * presents / enrolled) : 0;

                // Photo 1
                String pictureDataUri = null;
                int pictureSizeKB = 0;
                if (r[8] != null) {
                    byte[] picBytes = (byte[]) r[8];
                    pictureSizeKB = picBytes.length / 1024;
                    pictureDataUri = buildDataUri(picBytes);
                }

                // Photo 2
                String pictureDataUri2 = null;
                int pictureSizeKB2 = 0;
                if (r[9] != null) {
                    byte[] picBytes2 = (byte[]) r[9];
                    pictureSizeKB2 = picBytes2.length / 1024;
                    pictureDataUri2 = buildDataUri(picBytes2);
                }

                sessions.add(new SessionRow(
                        attID, (String) r[1], (String) r[2],
                        date, start, end, enrolled, presents, rate,
                        pictureDataUri, pictureSizeKB,
                        pictureDataUri2, pictureSizeKB2));
            }
        } finally { em.close(); }
    }

    private String buildDataUri(byte[] picBytes) {
        String mimeType = "image/jpeg";
        if (picBytes.length >= 4) {
            int b0 = picBytes[0] & 0xFF;
            int b1 = picBytes[1] & 0xFF;
            int b2 = picBytes[2] & 0xFF;
            int b3 = picBytes[3] & 0xFF;
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) mimeType = "image/png";
            else if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) mimeType = "image/gif";
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(picBytes);
    }

    public void viewDetail(String attendanceID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT s.matricule, s.firstName, s.lastName, sa.isPresent, "
                    + "  (SELECT COUNT(*) FROM studentattendance sa2 "
                    + "   JOIN attendance a2 ON a2.attendanceID = sa2.attendanceID "
                    + "   WHERE sa2.studentID = s.studentID "
                    + "   AND a2.courseID = (SELECT courseID FROM attendance WHERE attendanceID = ?) "
                    + "   AND sa2.isPresent = 0) AS totalAbsences "
                    + "FROM studentattendance sa "
                    + "JOIN student s ON s.studentID = sa.studentID "
                    + "WHERE sa.attendanceID = ? "
                    + "ORDER BY sa.isPresent DESC, s.lastName")
                    .setParameter(1, attendanceID)
                    .setParameter(2, attendanceID)
                    .getResultList();

            List<StudentLine> lines = new ArrayList<>();
            int p = 0, a = 0;
            for (Object[] r : rows) {
                boolean present = false;
                if (r[3] != null) {
                    String val = r[3].toString();
                    present = val.equals("1") || val.equalsIgnoreCase("true");
                }
                if (present) p++; else a++;
                int totalAbs = r[4] != null ? ((Number) r[4]).intValue() : 0;
                lines.add(new StudentLine((String) r[0], r[1] + " " + r[2], present, totalAbs));
            }
            selectedDetail = new SessionDetail(attendanceID, lines, p, a);
        } finally { em.close(); }
    }

    public void closeDetail() {
        selectedDetail = null;
    }

    public String refresh() {
        if (currentFaculty != null) {
            loadCourses();
            loadSessions();
        }
        return null;
    }

    public Faculty getCurrentFaculty() { return currentFaculty; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; loadSessions(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; loadSessions(); }
    public String getSelectedCourseID() { return selectedCourseID; }
    public void setSelectedCourseID(String s) { this.selectedCourseID = s; loadSessions(); }
    public List<CourseOption> getMyCourses() { return myCourses; }
    public List<SessionRow> getSessions() { return sessions; }
    public SessionDetail getSelectedDetail() { return selectedDetail; }

    public static class CourseOption implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String courseID, code, name;
        public CourseOption(String i, String c, String n) {
            this.courseID = i; this.code = c; this.name = n;
        }
        public String getCourseID() { return courseID; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getLabel() { return code + " - " + name; }
    }

    public static class SessionRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String attendanceID, courseCode, courseName, date, startTime, endTime;
        private final int enrolled, presents, pictureSizeKB, pictureSizeKB2;
        private final double rate;
        private final String pictureDataUri, pictureDataUri2;

        public SessionRow(String i, String cc, String cn, String d, String s, String e,
                          int en, int p, double r, String pu, int ps, String pu2, int ps2) {
            this.attendanceID = i; this.courseCode = cc; this.courseName = cn;
            this.date = d; this.startTime = s; this.endTime = e;
            this.enrolled = en; this.presents = p; this.rate = r;
            this.pictureDataUri = pu; this.pictureSizeKB = ps;
            this.pictureDataUri2 = pu2; this.pictureSizeKB2 = ps2;
        }
        public String getAttendanceID() { return attendanceID; }
        public String getCourseCode() { return courseCode; }
        public String getCourseName() { return courseName; }
        public String getDate() { return date; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public int getEnrolled() { return enrolled; }
        public int getPresents() { return presents; }
        public int getAbsents() { return enrolled - presents; }
        public double getRate() { return rate; }
        public String getFormattedRate() { return String.format(java.util.Locale.US, "%.1f%%", rate); }
        public String getPictureDataUri() { return pictureDataUri; }
        public int getPictureSizeKB() { return pictureSizeKB; }
        public boolean isHasPicture() { return pictureDataUri != null; }
        public String getPictureDataUri2() { return pictureDataUri2; }
        public int getPictureSizeKB2() { return pictureSizeKB2; }
        public boolean isHasPicture2() { return pictureDataUri2 != null; }
        public String getRateColor() {
            if (rate >= 85) return "#16a34a";
            if (rate >= 70) return "#3b82f6";
            if (rate >= 55) return "#f59e0b";
            return "#dc2626";
        }
    }

    public static class SessionDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String attendanceID;
        private final List<StudentLine> lines;
        private final int totalPresents, totalAbsents;
        public SessionDetail(String i, List<StudentLine> l, int p, int a) {
            this.attendanceID = i; this.lines = l;
            this.totalPresents = p; this.totalAbsents = a;
        }
        public String getAttendanceID() { return attendanceID; }
        public List<StudentLine> getLines() { return lines; }
        public int getTotalPresents() { return totalPresents; }
        public int getTotalAbsents() { return totalAbsents; }
    }

    public static class StudentLine implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String matricule, fullName;
        private final boolean present;
        private final int totalAbsences;
        public StudentLine(String m, String f, boolean p, int ta) {
            this.matricule = m; this.fullName = f;
            this.present = p; this.totalAbsences = ta;
        }
        public String getMatricule() { return matricule; }
        public String getFullName() { return fullName; }
        public boolean isPresent() { return present; }
        public int getTotalAbsences() { return totalAbsences; }
        public String getAbsenceColor() {
            if (totalAbsences >= 5) return "#dc2626";
            if (totalAbsences == 4) return "#f59e0b";
            if (totalAbsences == 3) return "#3b82f6";
            return "#16a34a";
        }
    }
}