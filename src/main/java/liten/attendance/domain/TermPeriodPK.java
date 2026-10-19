package liten.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TermPeriodPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "termID", nullable = false, length = 15)
    private String termID;

    @Column(name = "periodID", nullable = false, length = 15)
    private String periodID;

    public TermPeriodPK() {}

    public TermPeriodPK(String termID, String periodID) {
        this.termID = termID;
        this.periodID = periodID;
    }

    public String getTermID() { return termID; }
    public void setTermID(String termID) { this.termID = termID; }

    public String getPeriodID() { return periodID; }
    public void setPeriodID(String periodID) { this.periodID = periodID; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TermPeriodPK)) return false;
        TermPeriodPK that = (TermPeriodPK) o;
        return Objects.equals(termID, that.termID)
                && Objects.equals(periodID, that.periodID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(termID, periodID);
    }
}