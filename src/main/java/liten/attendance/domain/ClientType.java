package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Type de client utilisateur.
 * Valeurs possibles : Student, Parent, Faculty, Manager, Employee.
 * Correspond à la table de référence `clienttype`.
 *
 * @author Liten
 */
@Entity
@Table(name = "clienttype")
public class ClientType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "clientTypeID")
    private String clientTypeID;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    // Constructeur vide obligatoire pour JPA
    public ClientType() {
    }

    // Getters / Setters

    public String getClientTypeID() { return clientTypeID; }
    public void setClientTypeID(String clientTypeID) { this.clientTypeID = clientTypeID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "ClientType{" + name + "}";
    }
}