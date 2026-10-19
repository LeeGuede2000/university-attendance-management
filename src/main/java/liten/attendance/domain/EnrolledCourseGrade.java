package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrolledcoursegrade")
public class EnrolledCourseGrade implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "enrolledCourseGradeID", length = 15)
    private String enrolledCourseGradeID;

    @Column(name = "studentID", length = 15)
    private String studentID;

    @Column(name = "courseID", length = 15)
    private String courseID;

    @Column(name = "termID", length = 15)
    private String termID;

    @Column(name = "periodID", length = 15)
    private String periodID;

    @Column(name = "finalGrade", length = 2)
    private String finalGrade;

    @Column(name = "numericGrade")
    private Double numericGrade;

    @Column(name = "totalPresences")
    private Integer totalPresences;

    @Column(name = "totalAbsences")
    private Integer totalAbsences;

    @Column(name = "isExcluded")
    private Boolean isExcluded;

    @Column(name = "createdDate")
    private LocalDateTime createdDate;

    @Column(name = "modifiedDate")
    private LocalDateTime modifiedDate;

    public EnrolledCourseGrade() {}

    public String getEnrolledCourseGradeID() { return enrolledCourseGradeID; }
    public void setEnrolledCourseGradeID(String id) { this.enrolledCourseGradeID = id; }
    public String getStudentID() { return studentID; }
    public void setStudentID(String s) { this.studentID = s; }
    public String getCourseID() { return courseID; }
    public void setCourseID(String c) { this.courseID = c; }
    public String getTermID() { return termID; }
    public void setTermID(String t) { this.termID = t; }
    public String getPeriodID() { return periodID; }
    public void setPeriodID(String p) { this.periodID = p; }
    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String f) { this.finalGrade = f; }
    public Double getNumericGrade() { return numericGrade; }
    public void setNumericGrade(Double n) { this.numericGrade = n; }
    public Integer getTotalPresences() { return totalPresences; }
    public void setTotalPresences(Integer p) { this.totalPresences = p; }
    public Integer getTotalAbsences() { return totalAbsences; }
    public void setTotalAbsences(Integer a) { this.totalAbsences = a; }
    public Boolean getIsExcluded() { return isExcluded; }
    public void setIsExcluded(Boolean e) { this.isExcluded = e; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime d) { this.createdDate = d; }
    public LocalDateTime getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(LocalDateTime d) { this.modifiedDate = d; }
}