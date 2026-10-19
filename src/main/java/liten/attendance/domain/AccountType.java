package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Type de compte utilisateur (Admin, Client, Employee).
 * Correspond à la table de référence `accounttype`.
 *
 * @author Liten
 */
@Entity
@Table(name = "accounttype")
public class AccountType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "accountTypeID")
    private String accountTypeID;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    // Constructeur vide obligatoire pour JPA
    public AccountType() {
    }

    // Getters / Setters

    public String getAccountTypeID() { return accountTypeID; }
    public void setAccountTypeID(String accountTypeID) { this.accountTypeID = accountTypeID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "AccountType{" + name + "}";
    }
}