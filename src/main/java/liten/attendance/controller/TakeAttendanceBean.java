package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import liten.attendance.dao.AttendanceDAO;
import liten.attendance.dao.CourseDAO;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.domain.Attendance;
import liten.attendance.domain.Course;
import liten.attendance.domain.Faculty;
import liten.attendance.domain.Notification;
import liten.attendance.domain.Student;
import liten.attendance.domain.StudentAttendance;
import liten.attendance.domain.StudentAttendancePK;
import liten.attendance.service.RulesEngineService;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;
import org.primefaces.model.file.UploadedFile;

/**
 * Backing bean de la page "Prise de présence" (côté faculty).
 *
 * Permet à un prof de :
 *   1. Sélectionner un cours qui lui est assigné
 *   2. Voir la liste des étudiants enrôlés
 *   3. Cocher présents/absents/en retard
 *   4. Uploader une photo de la liste signée
 *   5. Valider → sauvegarde + déclenche les règles automatiques 3/4/5
 *
 * @author Liten
 */
@Named("takeAttendanceBean")
@SessionScoped
public class TakeAttendanceBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final RulesEngineService rulesEngineService = new RulesEngineService();

    private Faculty currentFaculty;
    private List<Course> availableCourses = new ArrayList<>();
    private List<StudentAttendanceRow> studentRows = new ArrayList<>();

    // ---- Champs du formulaire ----
    private String selectedCourseId;
    private LocalDate sessionDate = LocalDate.now();
    private LocalTime startHour = LocalTime.of(8, 0);
    private LocalTime endHour = LocalTime.of(10, 0);
    private UploadedFile uploadedPicture;
    private UploadedFile uploadedPicture2;

    @PostConstruct
    public void init() {
        resolveCurrentFaculty();
        if (currentFaculty != null) {
            availableCourses = courseDAO.findCoursesByFaculty(currentFaculty.getFacultyID());
        }
    }

    private void resolveCurrentFaculty() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) {
            return;
        }
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID == null) return;
        currentFaculty = facultyDAO.findById(personID);
    }

    /** Listener appelé quand le prof sélectionne un cours dans le menu. */
    public void onCourseSelected() {
        studentRows.clear();
        if (selectedCourseId == null || selectedCourseId.isBlank()) {
            return;
        }
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<String> studentIds = em.createNativeQuery(
                    "SELECT DISTINCT studentID FROM enrollment "
                    + "WHERE courseID = ? AND isValid = 1")
                    .setParameter(1, selectedCourseId)
                    .getResultList();

            for (String sid : studentIds) {
                Student s = em.find(Student.class, sid);
                if (s != null) {
                    studentRows.add(new StudentAttendanceRow(s));
                }
            }
        } finally {
            em.close();
        }
    }

    /**
     * Valide la prise de présence :
     *  1. Crée Attendance + StudentAttendance pour chaque étudiant
     *  2. Sauvegarde en base
     *  3. Déclenche le moteur de règles pour chaque étudiant absent
     */
    public String validateAttendance() {
        if (selectedCourseId == null || selectedCourseId.isBlank()) {
            addError("Veuillez sélectionner un cours.");
            return null;
        }
        if (studentRows.isEmpty()) {
            addError("Aucun étudiant à enregistrer.");
            return null;
        }

        // Charger le Course sélectionné
        Course selectedCourse = courseDAO.findById(selectedCourseId);
        if (selectedCourse == null) {
            addError("Cours introuvable.");
            return null;
        }

        try {
            // Créer l'Attendance (entête de la séance)
            Attendance attendance = new Attendance();
            attendance.setAttendanceID("ATT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            attendance.setCourse(selectedCourse);
            attendance.setFaculty(currentFaculty);
            attendance.setAttendanceDate(sessionDate);
            attendance.setStartHour(startHour);
            attendance.setEndHour(endHour);
            attendance.setTotalEnrolled(studentRows.size());

            int presents = 0;
            for (StudentAttendanceRow row : studentRows) {
                if (row.isPresent()) presents++;
            }
            attendance.setTotalPresents(presents);
            attendance.setIsValidated(true);

            // Lire la photo si uploadée
            if (uploadedPicture != null && uploadedPicture.getSize() > 0) {
                try (InputStream is = uploadedPicture.getInputStream()) {
                    attendance.setAssistancePicture(is.readAllBytes());
                } catch (IOException e) {
                    addError("Erreur de lecture de la photo : " + e.getMessage());
                    return null;
                }
            }
            
            if (uploadedPicture2 != null && uploadedPicture2.getSize() > 0) {
    try (InputStream is2 = uploadedPicture2.getInputStream()) {
        attendance.setAssistancePicture2(is2.readAllBytes());
    } catch (IOException e) {
        addError("Erreur de lecture de la photo 2 : " + e.getMessage());
        return null;
    }
}

            // Créer les lignes StudentAttendance
            List<StudentAttendance> lines = new ArrayList<>();
            for (StudentAttendanceRow row : studentRows) {
                StudentAttendance sa = new StudentAttendance();
                sa.setId(new StudentAttendancePK(attendance.getAttendanceID(),
                                                  row.getStudent().getStudentID()));
                sa.setAttendance(attendance);
                sa.setStudent(row.getStudent());
                sa.setIsPresent(row.isPresent());
                sa.setIsLate(row.isLate());
                lines.add(sa);
            }

            // Sauvegarder Attendance + StudentAttendance
            attendanceDAO.saveAttendanceSession(attendance, lines);

            addInfo("Présence enregistrée avec succès (" + presents + "/"
                    + studentRows.size() + " présents)");

            // ========== ÉTAPE 6 — MOTEUR DE RÈGLES ==========
            // Pour chaque étudiant absent, évaluer les règles 3/4/5
            int totalNotifications = 0;
            for (StudentAttendanceRow row : studentRows) {
                if (!row.isPresent()) {
                    List<Notification> notifs = rulesEngineService.evaluateForStudent(
                            row.getStudent(), selectedCourse);
                    totalNotifications += notifs.size();
                }
            }
            if (totalNotifications > 0) {
                addInfo(totalNotifications + " notification(s) automatique(s) déclenchée(s).");
            }
            // ===============================================

            // Reset
            selectedCourseId = null;
            studentRows.clear();
            uploadedPicture = null;
            uploadedPicture2 = null;

        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        }
        return null;
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    // Getters / Setters

    public Faculty getCurrentFaculty() { return currentFaculty; }
    public List<Course> getAvailableCourses() { return availableCourses; }
    public List<StudentAttendanceRow> getStudentRows() { return studentRows; }
    public String getSelectedCourseId() { return selectedCourseId; }
    public void setSelectedCourseId(String selectedCourseId) { this.selectedCourseId = selectedCourseId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartHour() { return startHour; }
    public void setStartHour(LocalTime startHour) { this.startHour = startHour; }
    public LocalTime getEndHour() { return endHour; }
    public void setEndHour(LocalTime endHour) { this.endHour = endHour; }
    public UploadedFile getUploadedPicture() { return uploadedPicture; }
    public void setUploadedPicture(UploadedFile uploadedPicture) { this.uploadedPicture = uploadedPicture; }

    /**
     * Classe interne représentant une ligne du tableau de présence.
     * Combine un étudiant + ses cases à cocher (présent / en retard).
     */
    public static class StudentAttendanceRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Student student;
        private boolean present = true;  // par défaut, présent
        private boolean late = false;

        public StudentAttendanceRow(Student student) {
            this.student = student;
        }

        public Student getStudent() { return student; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
        public boolean isLate() { return late; }
        public void setLate(boolean late) { this.late = late; }
    }
    
    public UploadedFile getUploadedPicture2() { return uploadedPicture2; }
    public void setUploadedPicture2(UploadedFile f) { this.uploadedPicture2 = f; }
}