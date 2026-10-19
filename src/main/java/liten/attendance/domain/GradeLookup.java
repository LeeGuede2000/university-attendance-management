package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "gradelookup")
public class GradeLookup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "gradeID", length = 15)
    private String gradeID;

    @Column(name = "valueRange", length = 80)
    private String valueRange;

    @Column(name = "minGradeValue")
    private Double minGradeValue;

    @Column(name = "maxGradeValue")
    private Double maxGradeValue;

    @Column(name = "maxIncluded")
    private Boolean maxIncluded;

    @Column(name = "value", length = 2)
    private String value;

    public GradeLookup() {}

    public String getGradeID() { return gradeID; }
    public void setGradeID(String g) { this.gradeID = g; }
    public String getValueRange() { return valueRange; }
    public void setValueRange(String v) { this.valueRange = v; }
    public Double getMinGradeValue() { return minGradeValue; }
    public void setMinGradeValue(Double m) { this.minGradeValue = m; }
    public Double getMaxGradeValue() { return maxGradeValue; }
    public void setMaxGradeValue(Double m) { this.maxGradeValue = m; }
    public Boolean getMaxIncluded() { return maxIncluded; }
    public void setMaxIncluded(Boolean m) { this.maxIncluded = m; }
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
}