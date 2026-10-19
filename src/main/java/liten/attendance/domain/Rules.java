package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "rules")
public class Rules implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "rulesID")
    private String rulesID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rulesTypeID")
    private RulesType rulesType;

    private String name;

    @Column(length = 500)
    private String description;

    private Integer maxAllowedAbsences;
    private Integer triggerAbsenceCount;

    @Column(name = "actionType")
    private String actionType;

    private Boolean isActive;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public Rules() {}

    public String getRulesID() { return rulesID; }
    public void setRulesID(String r) { this.rulesID = r; }
    public RulesType getRulesType() { return rulesType; }
    public void setRulesType(RulesType r) { this.rulesType = r; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public Integer getMaxAllowedAbsences() { return maxAllowedAbsences; }
    public void setMaxAllowedAbsences(Integer m) { this.maxAllowedAbsences = m; }
    public Integer getTriggerAbsenceCount() { return triggerAbsenceCount; }
    public void setTriggerAbsenceCount(Integer t) { this.triggerAbsenceCount = t; }
    public String getActionType() { return actionType; }
    public void setActionType(String a) { this.actionType = a; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean i) { this.isActive = i; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate c) { this.createdDate = c; }
}