package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite pour AbsenceJustification (studentID + attendanceID).
 */
@Embeddable
public class AbsenceJustificationPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "studentID")
    private String studentID;

    @Column(name = "attendanceID")
    private String attendanceID;

    public AbsenceJustificationPK() {
    }

    public AbsenceJustificationPK(String studentID, String attendanceID) {
        this.studentID = studentID;
        this.attendanceID = attendanceID;
    }

    public String getStudentID() { return studentID; }
    public void setStudentID(String studentID) { this.studentID = studentID; }

    public String getAttendanceID() { return attendanceID; }
    public void setAttendanceID(String attendanceID) { this.attendanceID = attendanceID; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbsenceJustificationPK)) return false;
        AbsenceJustificationPK that = (AbsenceJustificationPK) o;
        return Objects.equals(studentID, that.studentID)
                && Objects.equals(attendanceID, that.attendanceID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID, attendanceID);
    }
}