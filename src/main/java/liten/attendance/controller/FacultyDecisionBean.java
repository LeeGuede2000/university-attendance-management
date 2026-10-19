
package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.dao.JustificationDAO;
import liten.attendance.domain.AbsenceJustification;
import liten.attendance.domain.Faculty;
import liten.attendance.service.JustificationWorkflowService;
import liten.attendance.util.SessionManager;


/**
 * Backing bean de la page "Décision finale" (niveau prof).
 *
 * Le prof voit les justifications déjà acceptées par l'advisor
 * et prend la décision finale : accepter (absence annulée) ou
 * rejeter (absence maintenue).
 *
 * @author Liten
 */
@Named("facultyDecisionBean")
@SessionScoped
public class FacultyDecisionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final JustificationDAO justificationDAO = new JustificationDAO();
    private final JustificationWorkflowService workflowService = new JustificationWorkflowService();

    private Faculty currentFaculty;
    private List<AbsenceJustification> pendingDecisions = new ArrayList<>();

    private String selectedStudentId;
    private String selectedAttendanceId;
    private String facultyComments;

    @PostConstruct
    public void init() {
        resolveCurrentFaculty();
        loadPendingDecisions();
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

    private void loadPendingDecisions() {
        if (currentFaculty == null) {
            pendingDecisions = new ArrayList<>();
            return;
        }
        pendingDecisions = new ArrayList<>(
            justificationDAO.findPendingForFaculty(currentFaculty.getFacultyID())
        );
    }

    public void prepareDecision(String studentId, String attendanceId) {
        this.selectedStudentId = studentId;
        this.selectedAttendanceId = attendanceId;
        this.facultyComments = null;
    }

    public boolean decisionOpenFor(String studentId, String attendanceId) {
        return selectedStudentId != null && selectedStudentId.equals(studentId)
                && selectedAttendanceId != null && selectedAttendanceId.equals(attendanceId);
    }

    public String approveAbsence() {
        if (selectedStudentId == null || selectedAttendanceId == null) {
            addError("Aucune justification sélectionnée.");
            return null;
        }
        try {
            workflowService.facultyAccept(selectedStudentId, selectedAttendanceId,
                    facultyComments != null ? facultyComments : "");
            addInfo("Justification approuvée. L'absence est annulée.");
            resetForm();
            loadPendingDecisions();
        } catch (JustificationWorkflowService.BusinessException be) {
            addError(be.getMessage());
        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        }
        return null;
    }

    public String rejectAbsence() {
        if (selectedStudentId == null || selectedAttendanceId == null) {
            addError("Aucune justification sélectionnée.");
            return null;
        }
        if (facultyComments == null || facultyComments.isBlank()) {
            addError("Veuillez justifier votre rejet dans le commentaire.");
            return null;
        }
        try {
            workflowService.facultyReject(selectedStudentId, selectedAttendanceId, facultyComments);
            addInfo("Justification rejetée. L'absence est maintenue.");
            resetForm();
            loadPendingDecisions();
        } catch (JustificationWorkflowService.BusinessException be) {
            addError(be.getMessage());
        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        }
        return null;
    }

    public void cancelDecision() {
        resetForm();
    }

    public String refresh() {
        loadPendingDecisions();
        return null;
    }

    private void resetForm() {
        selectedStudentId = null;
        selectedAttendanceId = null;
        facultyComments = null;
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
    public List<AbsenceJustification> getPendingDecisions() { return pendingDecisions; }
    public String getSelectedStudentId() { return selectedStudentId; }
    public void setSelectedStudentId(String selectedStudentId) { this.selectedStudentId = selectedStudentId; }
    public String getSelectedAttendanceId() { return selectedAttendanceId; }
    public void setSelectedAttendanceId(String selectedAttendanceId) { this.selectedAttendanceId = selectedAttendanceId; }
    public String getFacultyComments() { return facultyComments; }
    public void setFacultyComments(String facultyComments) { this.facultyComments = facultyComments; }

    public String getProofDataUri(AbsenceJustification j) {
        if (j == null || j.getProofSupport() == null || j.getProofSupport().length == 0) return null;
        byte[] bytes = j.getProofSupport();
        String mime = "image/jpeg";
        if (bytes.length >= 4) {
            int b0 = bytes[0] & 0xFF, b1 = bytes[1] & 0xFF,
                b2 = bytes[2] & 0xFF, b3 = bytes[3] & 0xFF;
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) mime = "image/png";
            else if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) mime = "image/gif";
        }
        return "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }
}

