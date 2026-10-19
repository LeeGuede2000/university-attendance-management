package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Entité JPA représentant un cours (table `course`).
 *
 * @author Liten
 */
@Entity
@Table(name = "course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "courseID")
    private String courseID;

    @Column(name = "name")
    private String name;

    /** Code du cours, ex: CSC301, MTH101 */
    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    /** SSE ou BSS */
    @Column(name = "school")
    private String school;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public Course() {
    }

    public String getCourseID() { return courseID; }
    public void setCourseID(String courseID) { this.courseID = courseID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    @Override
    public String toString() {
        return "Course{" + code + " - " + name + "}";
    }
}