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

/**
 * Entité JPA représentant un compte utilisateur (table `account`).
 * Un compte est lié à un client (étudiant, parent, faculté, etc.).
 *
 * @author Liten
 */
@Entity
@Table(name = "account")
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "accountID")
    private String accountID;

    @Column(name = "userName", unique = true)
    private String userName;

    /** Mot de passe hashé en BCrypt (60 caractères). */
    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "isActive")
    private Boolean isActive;

    @Column(name = "failedAttempts")
    private Integer failedAttempts;

    @Column(name = "lastLoginDate")
    private LocalDateTime lastLoginDate;

    /** Lien vers le client associé (étudiant, parent, faculté...). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clientID")
    private Client client;

    /** Type de compte : Admin, Client, Employee. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "accountTypeID")
    private AccountType accountType;

    // Constructeur vide obligatoire pour JPA
    public Account() {
    }

    // Getters / Setters

    public String getAccountID() { return accountID; }
    public void setAccountID(String accountID) { this.accountID = accountID; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDateTime lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    @Override
    public String toString() {
        return "Account{userName=" + userName + ", status=" + status + "}";
    }
}