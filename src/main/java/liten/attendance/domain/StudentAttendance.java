package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Présence individuelle d'un étudiant à une session (table `studentattendance`).
 * Indique si l'étudiant était présent, en retard, etc.
 *
 * @author Liten
 */
@Entity
@Table(name = "studentattendance")
public class StudentAttendance implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private StudentAttendancePK id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("attendanceID")
    @JoinColumn(name = "attendanceID")
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("studentID")
    @JoinColumn(name = "studentID")
    private Student student;

    @Column(name = "isPresent")
    private Boolean isPresent;

    @Column(name = "isLate")
    private Boolean isLate;

    @Column(name = "comments")
    private String comments;

    @Column(name = "createdDate")
    private LocalDateTime createdDate;

    @Column(name = "modifiedDate")
    private LocalDateTime modifiedDate;

    public StudentAttendance() {
    }

    public StudentAttendancePK getId() { return id; }
    public void setId(StudentAttendancePK id) { this.id = id; }

    public Attendance getAttendance() { return attendance; }
    public void setAttendance(Attendance attendance) { this.attendance = attendance; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Boolean getIsPresent() { return isPresent; }
    public void setIsPresent(Boolean isPresent) { this.isPresent = isPresent; }

    public Boolean getIsLate() { return isLate; }
    public void setIsLate(Boolean isLate) { this.isLate = isLate; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }
}