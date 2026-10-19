package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "term")
public class Term implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "termID", nullable = false, length = 15)
    private String termID;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "shortName", length = 80)
    private String shortName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public Term() {}

    public String getTermID() { return termID; }
    public void setTermID(String termID) { this.termID = termID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
}