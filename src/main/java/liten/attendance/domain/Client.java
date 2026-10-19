package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Entité JPA représentant un client (table `client`).
 * Un client est l'entité commune à tous les utilisateurs.
 * Le champ personID pointe vers Student, Parent, Faculty ou Employee
 * selon le clientType.
 *
 * @author Liten
 */
@Entity
@Table(name = "client")
public class Client implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "clientID")
    private String clientID;

    /** Identifiant de la personne (peut être un studentID, parentID, facultyID ou employeeID). */
    @Column(name = "personID")
    private String personID;

    @Column(name = "email")
    private String email;

    @Column(name = "isValid")
    private Boolean isValid;

    @Column(name = "startDate")
    private LocalDate startDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    /** Type de client : Student, Parent, Faculty, Manager, Employee. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clientTypeID")
    private ClientType clientType;

    // Constructeur vide obligatoire pour JPA
    public Client() {
    }

    // Getters / Setters

    public String getClientID() { return clientID; }
    public void setClientID(String clientID) { this.clientID = clientID; }

    public String getPersonID() { return personID; }
    public void setPersonID(String personID) { this.personID = personID; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }

    @Override
    public String toString() {
        return "Client{" + clientID + ", type=" + (clientType != null ? clientType.getName() : "?") + "}";
    }
}