package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "action")
public class Action implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "actionID")
    private String actionID;

    private String name;

    @Column(name = "actionContent", length = 500)
    private String actionContent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "studentID")
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "courseID")
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "advisorID")
    private Faculty advisor;

    @Column(name = "nbrAbsences")
    private Integer nbrAbsences;

    @Column(name = "isCompleted")
    private Boolean isCompleted;

    @Column(name = "actionDate")
    private LocalDateTime actionDate;

    @Column(name = "createdDate")
    private LocalDateTime createdDate;

    public Action() {}

    public String getActionID() { return actionID; }
    public void setActionID(String a) { this.actionID = a; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getActionContent() { return actionContent; }
    public void setActionContent(String c) { this.actionContent = c; }
    public Student getStudent() { return student; }
    public void setStudent(Student s) { this.student = s; }
    public Course getCourse() { return course; }
    public void setCourse(Course c) { this.course = c; }
    public Faculty getAdvisor() { return advisor; }
    public void setAdvisor(Faculty a) { this.advisor = a; }
    public Integer getNbrAbsences() { return nbrAbsences; }
    public void setNbrAbsences(Integer n) { this.nbrAbsences = n; }
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean i) { this.isCompleted = i; }
    public LocalDateTime getActionDate() { return actionDate; }
    public void setActionDate(LocalDateTime a) { this.actionDate = a; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime c) { this.createdDate = c; }
}