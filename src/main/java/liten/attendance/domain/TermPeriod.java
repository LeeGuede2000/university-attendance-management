package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "termperiod")
public class TermPeriod implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private TermPeriodPK id;

    @Column(name = "startDate")
    private LocalDate startDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    public TermPeriod() {}

    public TermPeriodPK getId() { return id; }
    public void setId(TermPeriodPK id) { this.id = id; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}