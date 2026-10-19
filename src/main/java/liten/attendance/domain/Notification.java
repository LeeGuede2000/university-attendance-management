package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "notificationID")
    private String notificationID;

    @Column(name = "recipientID")
    private String recipientID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "studentID")
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "courseID")
    private Course course;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "notifType")
    private String notifType;

    @Column(name = "channel")
    private String channel;

    @Column(name = "isRead")
    private Boolean isRead;

    @Column(name = "isSent")
    private Boolean isSent;

    @Column(name = "sentDate")
    private LocalDateTime sentDate;

    @Column(name = "createdDate")
    private LocalDateTime createdDate;

    public Notification() {}

    public String getNotificationID() { return notificationID; }
    public void setNotificationID(String notificationID) { this.notificationID = notificationID; }
    public String getRecipientID() { return recipientID; }
    public void setRecipientID(String recipientID) { this.recipientID = recipientID; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getNotifType() { return notifType; }
    public void setNotifType(String notifType) { this.notifType = notifType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public Boolean getIsSent() { return isSent; }
    public void setIsSent(Boolean isSent) { this.isSent = isSent; }
    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}