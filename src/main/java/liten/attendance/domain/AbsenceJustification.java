package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Justification d'absence d'un étudiant (table `absencejustification`).
 *
 * Workflow à 3 niveaux via le champ workflowStatus :
 *   Submitted     → l'étudiant a soumis sa justification
 *   AdvisorReview → en attente de validation par l'advisor
 *   FacultyReview → l'advisor a accepté, en attente de décision finale du prof
 *   Approved      → absence annulée
 *   Rejected      → absence maintenue
 *
 * @author Liten
 */
@Entity
@Table(name = "absencejustification")
public class AbsenceJustification implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private AbsenceJustificationPK id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("studentID")
    @JoinColumn(name = "studentID")
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("attendanceID")
    @JoinColumn(name = "attendanceID")
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "courseID")
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facultyID")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "advisorID")
    private Faculty advisor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "causeTypeID")
    private CauseType causeType;

    @Column(name = "reasons")
    private String reasons;

    @Lob
    @Column(name = "proofSupport")
    private byte[] proofSupport;

    @Column(name = "advisorComments")
    private String advisorComments;

    @Column(name = "facultyComments")
    private String facultyComments;

    @Column(name = "workflowStatus")
    private String workflowStatus;

    /** Décision de l'advisor : true=accepté, false=rejeté, null=pas encore décidé */
    @Column(name = "advisorDecision")
    private Boolean advisorDecision;

    @Column(name = "advisorDecisionDate")
    private LocalDateTime advisorDecisionDate;

    /** Décision finale du prof */
    @Column(name = "decision")
    private Boolean decision;

    @Column(name = "decisionDate")
    private LocalDateTime decisionDate;

    @Column(name = "createdDate")
    private LocalDateTime createdDate;

    @Column(name = "modifiedDate")
    private LocalDateTime modifiedDate;

    public AbsenceJustification() {
    }

    public AbsenceJustificationPK getId() { return id; }
    public void setId(AbsenceJustificationPK id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Attendance getAttendance() { return attendance; }
    public void setAttendance(Attendance attendance) { this.attendance = attendance; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public Faculty getAdvisor() { return advisor; }
    public void setAdvisor(Faculty advisor) { this.advisor = advisor; }

    public CauseType getCauseType() { return causeType; }
    public void setCauseType(CauseType causeType) { this.causeType = causeType; }

    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }

    public byte[] getProofSupport() { return proofSupport; }
    public void setProofSupport(byte[] proofSupport) { this.proofSupport = proofSupport; }

    public String getAdvisorComments() { return advisorComments; }
    public void setAdvisorComments(String advisorComments) { this.advisorComments = advisorComments; }

    public String getFacultyComments() { return facultyComments; }
    public void setFacultyComments(String facultyComments) { this.facultyComments = facultyComments; }

    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }

    public Boolean getAdvisorDecision() { return advisorDecision; }
    public void setAdvisorDecision(Boolean advisorDecision) { this.advisorDecision = advisorDecision; }

    public LocalDateTime getAdvisorDecisionDate() { return advisorDecisionDate; }
    public void setAdvisorDecisionDate(LocalDateTime advisorDecisionDate) { this.advisorDecisionDate = advisorDecisionDate; }

    public Boolean getDecision() { return decision; }
    public void setDecision(Boolean decision) { this.decision = decision; }

    public LocalDateTime getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDateTime decisionDate) { this.decisionDate = decisionDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }
}