package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite pour StudentAttendance (attendanceID + studentID).
 */
@Embeddable
public class StudentAttendancePK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "attendanceID")
    private String attendanceID;

    @Column(name = "studentID")
    private String studentID;

    public StudentAttendancePK() {
    }

    public StudentAttendancePK(String attendanceID, String studentID) {
        this.attendanceID = attendanceID;
        this.studentID = studentID;
    }

    public String getAttendanceID() { return attendanceID; }
    public void setAttendanceID(String attendanceID) { this.attendanceID = attendanceID; }

    public String getStudentID() { return studentID; }
    public void setStudentID(String studentID) { this.studentID = studentID; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentAttendancePK)) return false;
        StudentAttendancePK that = (StudentAttendancePK) o;
        return Objects.equals(attendanceID, that.attendanceID)
                && Objects.equals(studentID, that.studentID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attendanceID, studentID);
    }
}