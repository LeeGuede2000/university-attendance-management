package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Cause d'absence (table `causetype`).
 * Valeurs : Maladie, Famille, Transport, Personnelle, etc.
 *
 * @author Liten
 */
@Entity
@Table(name = "causetype")
public class CauseType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "causeTypeID")
    private String causeTypeID;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    public CauseType() {
    }

    public String getCauseTypeID() { return causeTypeID; }
    public void setCauseTypeID(String causeTypeID) { this.causeTypeID = causeTypeID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "CauseType{" + name + "}";
    }
}