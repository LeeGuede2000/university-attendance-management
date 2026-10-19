package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "period")
public class Period implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "periodID", nullable = false, length = 15)
    private String periodID;

    @Column(name = "startYear")
    private Integer startYear;

    @Column(name = "endYear")
    private Integer endYear;

    @Column(name = "createdDate")
    private LocalDate createdDate;

    public Period() {}

    public String getPeriodID() { return periodID; }
    public void setPeriodID(String periodID) { this.periodID = periodID; }

    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }

    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    /** Helper pour l'affichage : "2024-2025" */
    public String getDisplayName() {
        if (startYear == null || endYear == null) return periodID;
        return startYear + "-" + endYear;
    }
}