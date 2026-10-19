package liten.attendance.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "eventtype")
public class EventType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "eventTypeID", nullable = false, length = 50)
    private String eventTypeID;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "createdDate")
    private Date createdDate;

    public EventType() {}

    public String getEventTypeID() { return eventTypeID; }
    public void setEventTypeID(String eventTypeID) { this.eventTypeID = eventTypeID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
}