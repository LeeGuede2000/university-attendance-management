package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.domain.Faculty;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("gradesBean")
@SessionScoped
public class GradesBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final FacultyDAO facultyDAO = new FacultyDAO();

    private Faculty currentFaculty;
    private String selectedCourseID;
    private String selectedTermID = "TERM_1";
    private String selectedPeriodID = "PER_3";

    private List<CourseOption> myCourses = new ArrayList<>();
    private List<StudentGradeRow> students = new ArrayList<>();

    @PostConstruct
    public void init() {
        resolveCurrent();
        loadMyCourses();
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID != null) currentFaculty = facultyDAO.findById(personID);
    }

    private void loadMyCourses() {
        myCourses.clear();
        if (currentFaculty == null) return;
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
                myCourses.add(new CourseOption(
                        (String) r[0], (String) r[1], (String) r[2]));
            }
        } finally {
            em.close();
        }
    }

    public void onCourseSelected() {
        loadStudents();
    }

    private void loadStudents() {
    students.clear();
    if (selectedCourseID == null || selectedCourseID.isBlank()) return;

    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT s.studentID, s.firstName, s.lastName, s.matricule, "
                + "  COUNT(sa.attendanceID) AS totalSessions, "
                + "  SUM(CASE WHEN sa.isPresent = 1 THEN 1 ELSE 0 END) AS presents, "
                + "  SUM(CASE WHEN sa.isPresent = 0 THEN 1 ELSE 0 END) AS absences "
                + "FROM studentattendance sa "
                + "JOIN student s ON s.studentID = sa.studentID "
                + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                + "JOIN termperiod tp ON tp.termID = ? AND tp.periodID = ? "
                + "WHERE a.courseID = ? "
                + "  AND a.attendanceDate BETWEEN tp.startDate AND tp.endDate "
                + "GROUP BY s.studentID, s.firstName, s.lastName, s.matricule "
                + "ORDER BY s.lastName, s.firstName")
                .setParameter(1, selectedTermID)
                .setParameter(2, selectedPeriodID)
                .setParameter(3, selectedCourseID)
                .getResultList();

        for (Object[] r : rows) {
            String studentID = (String) r[0];
            int presences = r[5] != null ? ((Number) r[5]).intValue() : 0;
            int absences = r[6] != null ? ((Number) r[6]).intValue() : 0;

            StudentGradeRow row = new StudentGradeRow(
                    studentID, (String) r[1], (String) r[2], (String) r[3],
                    presences, absences);

            // Chercher une note existante (utilise getResultList pour éviter NoResultException)
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> gradeRows = em.createNativeQuery(
                        "SELECT enrolledCourseGradeID, numericGrade, finalGrade, isExcluded "
                        + "FROM enrolledcoursegrade "
                        + "WHERE studentID = ? AND courseID = ? AND termID = ? AND periodID = ?")
                        .setParameter(1, studentID)
                        .setParameter(2, selectedCourseID)
                        .setParameter(3, selectedTermID)
                        .setParameter(4, selectedPeriodID)
                        .getResultList();

                if (!gradeRows.isEmpty()) {
                    Object[] existing = gradeRows.get(0);
                    row.setExistingID((String) existing[0]);
                    if (existing[1] != null) {
                        row.setNumericGrade(((Number) existing[1]).doubleValue());
                    }
                    if (existing[2] != null) {
                        row.setFinalGrade(existing[2].toString());
                    }
                    if (existing[3] != null) {
                        row.setExcluded(existing[3].toString().equals("1")
                                || existing[3].toString().equalsIgnoreCase("true"));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Fallback : si lettre absente mais note présente, calcule depuis la note
            if (row.getFinalGrade() == null && row.getNumericGrade() != null) {
                row.setFinalGrade(computeLetterGrade(em, row.getNumericGrade()));
            }

            students.add(row);
        }
    } finally {
        em.close();
    }
}

    public String saveAll() {
        if (selectedCourseID == null || selectedCourseID.isBlank()) {
            addError("Sélectionnez un cours.");
            return null;
        }

        EntityManager em = DataManager.getEntityManager();
        int updated = 0;
        try {
            em.getTransaction().begin();
            for (StudentGradeRow row : students) {
                if (row.getNumericGrade() == null) continue; // skip non noté

                String letter = computeLetterGrade(em, row.getNumericGrade());
                boolean excluded = row.getAbsences() >= 5;

                if (row.getExistingID() != null) {
                    // UPDATE
                    em.createNativeQuery(
                            "UPDATE enrolledcoursegrade "
                            + "SET numericGrade = ?, finalGrade = ?, "
                            + "    totalPresences = ?, totalAbsences = ?, "
                            + "    isExcluded = ?, modifiedDate = ? "
                            + "WHERE enrolledCourseGradeID = ?")
                            .setParameter(1, row.getNumericGrade())
                            .setParameter(2, letter)
                            .setParameter(3, row.getPresences())
                            .setParameter(4, row.getAbsences())
                            .setParameter(5, excluded ? 1 : 0)
                            .setParameter(6, LocalDateTime.now())
                            .setParameter(7, row.getExistingID())
                            .executeUpdate();
                } else {
                    // INSERT
                    String newID = "ECG_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    em.createNativeQuery(
                            "INSERT INTO enrolledcoursegrade "
                            + "(enrolledCourseGradeID, studentID, courseID, termID, periodID, "
                            + " numericGrade, finalGrade, totalPresences, totalAbsences, isExcluded, "
                            + " createdDate, modifiedDate) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                            .setParameter(1, newID)
                            .setParameter(2, row.getStudentID())
                            .setParameter(3, selectedCourseID)
                            .setParameter(4, selectedTermID)
                            .setParameter(5, selectedPeriodID)
                            .setParameter(6, row.getNumericGrade())
                            .setParameter(7, letter)
                            .setParameter(8, row.getPresences())
                            .setParameter(9, row.getAbsences())
                            .setParameter(10, excluded ? 1 : 0)
                            .setParameter(11, LocalDateTime.now())
                            .setParameter(12, LocalDateTime.now())
                            .executeUpdate();
                    row.setExistingID(newID);
                }
                row.setFinalGrade(letter);
                row.setExcluded(excluded);
                updated++;
            }
            em.getTransaction().commit();
            addInfo(updated + " note(s) enregistrée(s).");
            loadStudents();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            addError("Erreur : " + e.getMessage());
        } finally {
            em.close();
        }
        return null;
    }

    private String computeLetterGrade(EntityManager em, Double numeric) {
        if (numeric == null) return null;
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT value, minGradeValue, maxGradeValue, maxIncluded "
                    + "FROM gradelookup "
                    + "WHERE valueRange LIKE '[%]' OR valueRange LIKE '[%[' "
                    + "ORDER BY minGradeValue DESC")
                    .getResultList();
            for (Object[] r : rows) {
                double min = ((Number) r[1]).doubleValue();
                double max = ((Number) r[2]).doubleValue();
                boolean maxIncl = r[3] != null && r[3].toString().equals("1");
                if (numeric >= min && (maxIncl ? numeric <= max : numeric < max)) {
                    return (String) r[0];
                }
            }
        } catch (Exception e) { /* fallback below */ }
        // Fallback simple
       // Fallback US complet
        if (numeric >= 97) return "A+";
        if (numeric >= 93) return "A";
        if (numeric >= 90) return "A-";
        if (numeric >= 87) return "B+";
        if (numeric >= 83) return "B";
        if (numeric >= 80) return "B-";
        if (numeric >= 77) return "C+";
        if (numeric >= 73) return "C";
        if (numeric >= 70) return "C-";
        if (numeric >= 59.5) return "D";
         return "F";
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    public Faculty getCurrentFaculty() { return currentFaculty; }
    public String getSelectedCourseID() { return selectedCourseID; }
    public void setSelectedCourseID(String s) { this.selectedCourseID = s; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; loadStudents(); }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; loadStudents(); }
    public List<CourseOption> getMyCourses() { return myCourses; }
    public List<StudentGradeRow> getStudents() { return students; }

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

    public static class StudentGradeRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String studentID, firstName, lastName, matricule;
        private final int presences, absences;
        private Double numericGrade;
        private String finalGrade;
        private boolean excluded;
        private String existingID;

        public StudentGradeRow(String id, String f, String l, String m, int p, int a) {
            this.studentID = id; this.firstName = f; this.lastName = l;
            this.matricule = m; this.presences = p; this.absences = a;
        }
        public String getStudentID() { return studentID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getMatricule() { return matricule; }
        public int getPresences() { return presences; }
        public int getAbsences() { return absences; }
        public Double getNumericGrade() { return numericGrade; }
        public void setNumericGrade(Double n) { this.numericGrade = n; }
        public String getFinalGrade() { return finalGrade; }
        public void setFinalGrade(String f) { this.finalGrade = f; }
        public boolean isExcluded() { return excluded; }
        public void setExcluded(boolean e) { this.excluded = e; }
        public String getExistingID() { return existingID; }
        public void setExistingID(String e) { this.existingID = e; }
        public String getStatus() {
            if (absences >= 5) return "EXCLUSION";
            if (absences == 4) return "CONVOCATION";
            if (absences == 3) return "PARENT_ALERT";
            return "OK";
        }
        public String getStatusColor() {
            if (absences >= 5) return "#dc2626";
            if (absences == 4) return "#f59e0b";
            if (absences == 3) return "#3b82f6";
            return "#16a34a";
        }
    }
}