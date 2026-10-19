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
import java.util.ArrayList;
import java.util.List;
import liten.attendance.dao.AttendanceDAO;
import liten.attendance.dao.JustificationDAO;
import liten.attendance.domain.AbsenceJustification;
import liten.attendance.domain.Attendance;
import liten.attendance.domain.CauseType;
import liten.attendance.domain.Faculty;
import liten.attendance.domain.Student;
import liten.attendance.domain.StudentAttendance;
import liten.attendance.service.JustificationWorkflowService;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;
import org.primefaces.model.file.UploadedFile;

/**
 * Backing bean de la page "Mes absences" (côté étudiant).
 *
 * Permet à l'étudiant de :
 *   1. Consulter toutes ses absences du semestre
 *   2. Voir le statut de chaque justification (s'il y en a une)
 *   3. Soumettre une nouvelle justification pour une absence
 *
 * @author Liten
 */
@Named("studentBean")
@SessionScoped
public class StudentBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final JustificationDAO justificationDAO = new JustificationDAO();
    private final JustificationWorkflowService workflowService = new JustificationWorkflowService();

    /** L'étudiant connecté (résolu depuis la session) */
    private Student currentStudent;

    /** Toutes les absences de l'étudiant (lignes studentattendance avec isPresent=false) */
    private List<AbsenceRow> absences = new ArrayList<>();

    /** Liste des causes possibles (Maladie, Famille...) pour le formulaire */
    private List<CauseType> availableCauses = new ArrayList<>();

    // ---- Champs du formulaire de justification ----
    private String selectedAttendanceId;
    private String selectedCauseTypeId;
    private String justificationReason;
    private UploadedFile proofFile;

    @PostConstruct
    public void init() {
        resolveCurrentStudent();
        if (currentStudent != null) {
            loadAbsences();
            loadCauses();
        }
    }

    /** Résout l'étudiant connecté via le personID du client lié au compte. */
    private void resolveCurrentStudent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) {
            return;
        }
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID == null) return;

        EntityManager em = DataManager.getEntityManager();
        try {
            currentStudent = em.find(Student.class, personID);
        } finally {
            em.close();
        }
    }

    /** Charge toutes les absences + leur justification (s'il y en a une). */
    private void loadAbsences() {
        absences.clear();
        if (currentStudent == null) return;

        List<StudentAttendance> all = attendanceDAO.findByStudent(currentStudent.getStudentID());
        for (StudentAttendance sa : all) {
            if (Boolean.FALSE.equals(sa.getIsPresent())) {
                AbsenceJustification existingJustif =
                        justificationDAO.findById(currentStudent.getStudentID(),
                                                  sa.getAttendance().getAttendanceID());
                absences.add(new AbsenceRow(sa, existingJustif));
            }
        }
    }

    /** Charge la liste des causes depuis la table causetype. */
    private void loadCauses() {
        EntityManager em = DataManager.getEntityManager();
        try {
            availableCauses = em.createQuery(
                    "SELECT c FROM CauseType c ORDER BY c.name", CauseType.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Préremplit le formulaire pour une absence donnée
     * (appelé quand l'étudiant clique "Justifier").
     */
    public void prepareJustification(String attendanceId) {
        this.selectedAttendanceId = attendanceId;
        this.selectedCauseTypeId = null;
        this.justificationReason = null;
        this.proofFile = null;
    }

    /**
     * Soumet la justification d'absence (appel du workflow service).
     */
    public String submitJustification() {
        if (selectedAttendanceId == null || selectedCauseTypeId == null
                || justificationReason == null || justificationReason.isBlank()) {
            addError("Veuillez choisir une cause et expliquer la raison.");
            return null;
        }

        EntityManager em = DataManager.getEntityManager();
        try {
            Attendance attendance = em.find(Attendance.class, selectedAttendanceId);
            CauseType cause = em.find(CauseType.class, selectedCauseTypeId);

            if (attendance == null || cause == null) {
                addError("Données invalides.");
                return null;
            }

            // Récupérer l'advisor de l'étudiant via SQL natif
            String advisorID = (String) em.createNativeQuery(
                    "SELECT facultyID FROM academicadvisor WHERE studentID = ? LIMIT 1")
                    .setParameter(1, currentStudent.getStudentID())
                    .getSingleResult();

            Faculty advisor = em.find(Faculty.class, advisorID);
            if (advisor == null) {
                addError("Aucun advisor trouvé pour cet étudiant.");
                return null;
            }

            // Lire le fichier de preuve s'il existe
            byte[] proofBytes = null;
            if (proofFile != null && proofFile.getSize() > 0) {
                try (InputStream is = proofFile.getInputStream()) {
                    proofBytes = is.readAllBytes();
                } catch (IOException e) {
                    addError("Erreur de lecture du fichier : " + e.getMessage());
                    return null;
                }
            }

            // Appel du workflow
            workflowService.submitJustification(
                    currentStudent, attendance, advisor, cause,
                    justificationReason, proofBytes);

            addInfo("Justification soumise avec succès. En attente de validation par l'advisor.");

            // Reset formulaire et reload
            selectedAttendanceId = null;
            selectedCauseTypeId = null;
            justificationReason = null;
            proofFile = null;
            loadAbsences();

        } catch (JustificationWorkflowService.BusinessException be) {
            addError(be.getMessage());
        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        } finally {
            em.close();
        }
        return null;
    }

    public void cancelJustification() {
        selectedAttendanceId = null;
        selectedCauseTypeId = null;
        justificationReason = null;
        proofFile = null;
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

    public Student getCurrentStudent() { return currentStudent; }
    public List<AbsenceRow> getAbsences() { return absences; }
    public List<CauseType> getAvailableCauses() { return availableCauses; }
    public String getSelectedAttendanceId() { return selectedAttendanceId; }
    public void setSelectedAttendanceId(String selectedAttendanceId) { this.selectedAttendanceId = selectedAttendanceId; }
    public String getSelectedCauseTypeId() { return selectedCauseTypeId; }
    public void setSelectedCauseTypeId(String selectedCauseTypeId) { this.selectedCauseTypeId = selectedCauseTypeId; }
    public String getJustificationReason() { return justificationReason; }
    public void setJustificationReason(String justificationReason) { this.justificationReason = justificationReason; }
    public UploadedFile getProofFile() { return proofFile; }
    public void setProofFile(UploadedFile proofFile) { this.proofFile = proofFile; }

    /**
     * Classe interne représentant une ligne d'absence affichée à l'étudiant.
     */
    public static class AbsenceRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final StudentAttendance attendance;
        private final AbsenceJustification justification;

        public AbsenceRow(StudentAttendance attendance, AbsenceJustification justification) {
            this.attendance = attendance;
            this.justification = justification;
        }

        public StudentAttendance getAttendance() { return attendance; }
        public AbsenceJustification getJustification() { return justification; }

        public boolean isHasJustification() {
            return justification != null;
        }

        public String getStatusLabel() {
            if (justification == null) return "Non justifiée";
            switch (justification.getWorkflowStatus()) {
                case "Submitted":      return "Soumise";
                case "AdvisorReview":  return "En attente advisor";
                case "FacultyReview":  return "En attente prof";
                case "Approved":       return "Approuvée";
                case "Rejected":       return "Rejetée";
                default:               return justification.getWorkflowStatus();
            }
        }

        public String getStatusColor() {
            if (justification == null) return "#dc2626";
            switch (justification.getWorkflowStatus()) {
                case "Approved":       return "#16a34a";
                case "Rejected":       return "#6b7280";
                case "FacultyReview":  return "#3b82f6";
                default:               return "#f59e0b";
            }
        }
    }
}