package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "rulestype")
public class RulesType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "rulesTypeID")
    private String rulesTypeID;

    private String name;
    private String description;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public RulesType() {}

    public String getRulesTypeID() { return rulesTypeID; }
    public void setRulesTypeID(String r) { this.rulesTypeID = r; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate c) { this.createdDate = c; }
}