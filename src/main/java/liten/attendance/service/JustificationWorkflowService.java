package liten.attendance.service;

import java.time.LocalDateTime;
import liten.attendance.dao.AttendanceDAO;
import liten.attendance.dao.JustificationDAO;
import liten.attendance.domain.AbsenceJustification;
import liten.attendance.domain.AbsenceJustificationPK;
import liten.attendance.domain.Attendance;
import liten.attendance.domain.CauseType;
import liten.attendance.domain.Faculty;
import liten.attendance.domain.Student;

/**
 * Service métier du workflow de justification d'absence.
 *
 * Implémente les 3 niveaux du workflow décrit dans le cahier des charges :
 *   1. L'étudiant soumet une justification     → status = Submitted
 *   2. L'advisor examine (1er niveau)
 *      - accepte                                → status = FacultyReview
 *      - rejette                                → status = Rejected (final)
 *   3. Le prof tranche en final (2e niveau)
 *      - accepte                                → status = Approved (absence annulée)
 *      - rejette                                → status = Rejected (absence maintenue)
 *
 * @author Liten
 */
public class JustificationWorkflowService {

    /** États possibles du workflow */
    public static final String STATUS_SUBMITTED = "Submitted";
    public static final String STATUS_ADVISOR_REVIEW = "AdvisorReview";
    public static final String STATUS_FACULTY_REVIEW = "AdvisorApproved";    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";

    private final JustificationDAO justificationDAO = new JustificationDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    // ========================================================================
    // ÉTAPE 1 : L'étudiant soumet une justification
    // ========================================================================

    /**
     * L'étudiant soumet une justification pour une absence.
     *
     * @param student    l'étudiant qui soumet
     * @param attendance la session d'absence concernée
     * @param advisor    l'advisor qui devra examiner
     * @param causeType  la cause de l'absence
     * @param reasons    explication détaillée
     * @param proof      preuve éventuelle (photo, certificat médical, etc.)
     * @return la justification créée
     * @throws BusinessException si une justification existe déjà
     */
    public AbsenceJustification submitJustification(
            Student student,
            Attendance attendance,
            Faculty advisor,
            CauseType causeType,
            String reasons,
            byte[] proof) {

        // Vérifier qu'il n'y a pas déjà une justification pour cette absence
        AbsenceJustification existing = justificationDAO.findById(
                student.getStudentID(), attendance.getAttendanceID());
        if (existing != null) {
            throw new BusinessException(
                "Une justification existe déjà pour cette absence (statut : "
                + existing.getWorkflowStatus() + ")");
        }

        AbsenceJustification j = new AbsenceJustification();
        j.setId(new AbsenceJustificationPK(
                student.getStudentID(), attendance.getAttendanceID()));
        j.setStudent(student);
        j.setAttendance(attendance);
        j.setCourse(attendance.getCourse());
        j.setFaculty(attendance.getFaculty());
        j.setAdvisor(advisor);
        j.setCauseType(causeType);
        j.setReasons(reasons);
        j.setProofSupport(proof);
        j.setWorkflowStatus(STATUS_SUBMITTED);
        j.setCreatedDate(LocalDateTime.now());

        justificationDAO.save(j);
        return j;
    }

    // ========================================================================
    // ÉTAPE 2 : L'advisor examine (1er niveau de validation)
    // ========================================================================

    /**
     * L'advisor accepte la justification → passage au niveau prof.
     */
    public void advisorAccept(String studentID, String attendanceID, String comments) {
        AbsenceJustification j = loadOrFail(studentID, attendanceID);
        ensureCanBeReviewedByAdvisor(j);

        j.setAdvisorDecision(Boolean.TRUE);
        j.setAdvisorDecisionDate(LocalDateTime.now());
        j.setAdvisorComments(comments);
        j.setWorkflowStatus(STATUS_FACULTY_REVIEW);
        j.setModifiedDate(LocalDateTime.now());

        justificationDAO.update(j);
    }

    /**
     * L'advisor rejette la justification → fin du workflow, absence maintenue.
     */
    public void advisorReject(String studentID, String attendanceID, String comments) {
        AbsenceJustification j = loadOrFail(studentID, attendanceID);
        ensureCanBeReviewedByAdvisor(j);

        j.setAdvisorDecision(Boolean.FALSE);
        j.setAdvisorDecisionDate(LocalDateTime.now());
        j.setAdvisorComments(comments);
        j.setWorkflowStatus(STATUS_REJECTED);
        j.setModifiedDate(LocalDateTime.now());

        justificationDAO.update(j);
    }

    // ========================================================================
    // ÉTAPE 3 : Le prof tranche en final (2e niveau, décision finale)
    // ========================================================================

    /**
     * Le prof accepte la justification → l'absence est annulée.
     */
    public void facultyAccept(String studentID, String attendanceID, String comments) {
        AbsenceJustification j = loadOrFail(studentID, attendanceID);
        ensureCanBeReviewedByFaculty(j);

        j.setDecision(Boolean.TRUE);
        j.setDecisionDate(LocalDateTime.now());
        j.setFacultyComments(comments);
        j.setWorkflowStatus(STATUS_APPROVED);
        j.setModifiedDate(LocalDateTime.now());

        justificationDAO.update(j);
    }

    /**
     * Le prof rejette la justification → l'absence est maintenue.
     */
    public void facultyReject(String studentID, String attendanceID, String comments) {
        AbsenceJustification j = loadOrFail(studentID, attendanceID);
        ensureCanBeReviewedByFaculty(j);

        j.setDecision(Boolean.FALSE);
        j.setDecisionDate(LocalDateTime.now());
        j.setFacultyComments(comments);
        j.setWorkflowStatus(STATUS_REJECTED);
        j.setModifiedDate(LocalDateTime.now());

        justificationDAO.update(j);
    }

    // ========================================================================
    // HELPERS PRIVÉS
    // ========================================================================

    private AbsenceJustification loadOrFail(String studentID, String attendanceID) {
        AbsenceJustification j = justificationDAO.findById(studentID, attendanceID);
        if (j == null) {
            throw new BusinessException("Justification introuvable");
        }
        return j;
    }

    private void ensureCanBeReviewedByAdvisor(AbsenceJustification j) {
        String status = j.getWorkflowStatus();
        if (!STATUS_SUBMITTED.equals(status) && !STATUS_ADVISOR_REVIEW.equals(status)) {
            throw new BusinessException(
                "Cette justification ne peut pas être examinée par l'advisor (statut : "
                + status + ")");
        }
    }

    private void ensureCanBeReviewedByFaculty(AbsenceJustification j) {
        if (!STATUS_FACULTY_REVIEW.equals(j.getWorkflowStatus())) {
            throw new BusinessException(
                "Cette justification ne peut pas être examinée par le prof (statut : "
                + j.getWorkflowStatus() + ")");
        }
    }

    // ========================================================================
    // EXCEPTION MÉTIER (classe interne)
    // ========================================================================

    /**
     * Exception levée quand une règle métier est violée.
     * Le contrôleur l'attrape et affiche le message à l'utilisateur.
     */
    public static class BusinessException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public BusinessException(String message) {
            super(message);
        }
    }
}