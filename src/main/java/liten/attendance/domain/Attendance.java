package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entité JPA représentant une session de présence (table `attendance`).
 * Chaque ligne = une séance de cours pour laquelle le prof a pris la présence.
 *
 * @author Liten
 */
@Entity
@Table(name = "attendance")
public class Attendance implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "attendanceID")
    private String attendanceID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "courseID")
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facultyID")
    private Faculty faculty;

    @Column(name = "attendanceType")
    private String attendanceType;

    @Column(name = "totalPresents")
    private Integer totalPresents;

    @Column(name = "totalEnrolled")
    private Integer totalEnrolled;

    /** Photo de la liste prise par le prof (preuve) */
    @Lob
    @Column(name = "assistancePicture")
    private byte[] assistancePicture;
    
    /** Deuxième photo de preuve (optionnel) */
    @Lob
    @Column(name = "assistancePicture2")
    private byte[] assistancePicture2;

    @Column(name = "attendanceDate")
    private LocalDate attendanceDate;

    @Column(name = "startHour")
    private LocalTime startHour;

    @Column(name = "endHour")
    private LocalTime endHour;

    /** true quand le prof a finalisé la prise de présence */
    @Column(name = "isValidated")
    private Boolean isValidated;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public Attendance() {
    }

    public String getAttendanceID() { return attendanceID; }
    public void setAttendanceID(String attendanceID) { this.attendanceID = attendanceID; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public String getAttendanceType() { return attendanceType; }
    public void setAttendanceType(String attendanceType) { this.attendanceType = attendanceType; }

    public Integer getTotalPresents() { return totalPresents; }
    public void setTotalPresents(Integer totalPresents) { this.totalPresents = totalPresents; }

    public Integer getTotalEnrolled() { return totalEnrolled; }
    public void setTotalEnrolled(Integer totalEnrolled) { this.totalEnrolled = totalEnrolled; }

    public byte[] getAssistancePicture() { return assistancePicture; }
    public void setAssistancePicture(byte[] assistancePicture) { this.assistancePicture = assistancePicture; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public LocalTime getStartHour() { return startHour; }
    public void setStartHour(LocalTime startHour) { this.startHour = startHour; }

    public LocalTime getEndHour() { return endHour; }
    public void setEndHour(LocalTime endHour) { this.endHour = endHour; }

    public Boolean getIsValidated() { return isValidated; }
    public void setIsValidated(Boolean isValidated) { this.isValidated = isValidated; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public byte[] getAssistancePicture2() { return assistancePicture2; }
public void setAssistancePicture2(byte[] assistancePicture2) { this.assistancePicture2 = assistancePicture2; }
    @Override
    public String toString() {
        return "Attendance{" + attendanceID + " on " + attendanceDate + "}";
    }
}