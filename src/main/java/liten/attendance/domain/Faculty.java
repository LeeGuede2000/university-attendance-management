package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Entité JPA représentant un professeur / faculty (table `faculty`).
 *
 * @author Liten
 */
@Entity
@Table(name = "faculty")
public class Faculty implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "facultyID")
    private String facultyID;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

   /** SSE ou BSS */
    @Column(name = "departmentName")
    private String departmentName;

    /** true si ce prof est aussi academic advisor */
    @Column(name = "isAdvisor")
    private Boolean isAdvisor;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public Faculty() {
    }

    public String getFacultyID() { return facultyID; }
    public void setFacultyID(String facultyID) { this.facultyID = facultyID; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Boolean getIsAdvisor() { return isAdvisor; }
    public void setIsAdvisor(Boolean isAdvisor) { this.isAdvisor = isAdvisor; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "Faculty{" + facultyID + " " + getFullName() + "}";
    }
}