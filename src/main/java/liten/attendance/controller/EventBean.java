package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import liten.attendance.dao.EventDAO;
import liten.attendance.domain.Event;
import liten.attendance.domain.EventType;
import liten.attendance.util.DataManager;

@Named("eventBean")
@SessionScoped
public class EventBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EventDAO eventDAO = new EventDAO();

    private List<Event> events = new ArrayList<>();
    private List<EventType> eventTypes = new ArrayList<>();

    private Event selected;
    private boolean editMode = false;
    private String selectedEventTypeID;

    @PostConstruct
    public void init() {
        loadEvents();
        loadEventTypes();
    }

    private void loadEvents() {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Event> result = em.createNativeQuery(
                    "SELECT * FROM event WHERE isActive = 1 OR isActive IS NULL ORDER BY createdDate DESC",
                    Event.class).getResultList();
            events = new ArrayList<>(result);
        } finally { em.close(); }
    }

    private void loadEventTypes() {
        EntityManager em = DataManager.getEntityManager();
        try {
            eventTypes = em.createQuery(
                    "SELECT et FROM EventType et ORDER BY et.name", EventType.class)
                    .getResultList();
        } finally { em.close(); }
    }

    public void prepareAdd() {
        selected = new Event();
        selected.setCreatedDate(new Date());
        selectedEventTypeID = null;
        editMode = false;
    }

    public void prepareEdit(Event event) {
        selected = event;
        selectedEventTypeID = (event.getEventType() != null)
                ? event.getEventType().getEventTypeID() : null;
        editMode = true;
    }

    public String save() {
        if (selected.getName() == null || selected.getName().isBlank()) {
            addError("Le nom de l'événement est obligatoire.");
            return null;
        }
        if (selectedEventTypeID != null && !selectedEventTypeID.isBlank()) {
            EntityManager em = DataManager.getEntityManager();
            try {
                selected.setEventType(em.find(EventType.class, selectedEventTypeID));
            } finally { em.close(); }
        } else {
            selected.setEventType(null);
        }
        if (selected.getScheduleID() != null) {
    String schedId = selected.getScheduleID().trim();
    if (schedId.isEmpty()) {
        selected.setScheduleID(null);
    } else {
        EntityManager em = DataManager.getEntityManager();
        try {
            Number count = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM schedule WHERE scheduleID = ?")
                .setParameter(1, schedId)
                .getSingleResult();
            if (count.intValue() == 0) {
                addError("Le Schedule ID '" + schedId + "' n'existe pas. Laissez le champ vide ou entrez un ID valide.");
                return null;
            }
        } finally { em.close(); }
    }
}
        try {
            if (editMode) {
                selected.setModifiedDate(new Date());
                eventDAO.update(selected);
                addInfo("Événement mis à jour.");
            } else {
                selected.setEventID("EVT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                selected.setModifiedDate(new Date());
                eventDAO.save(selected);
                addInfo("Événement créé.");
            }
            loadEvents();
            selected = null;
        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        }
        return null;
    }

    public String delete(Event event) {
        try {
            EntityManager em = DataManager.getEntityManager();
            try {
                em.getTransaction().begin();
                em.createNativeQuery("UPDATE event SET isActive = 0 WHERE eventID = ?")
                  .setParameter(1, event.getEventID())
                  .executeUpdate();
                em.getTransaction().commit();
            } finally { em.close(); }
            addInfo("Événement désactivé.");
            loadEvents();
        } catch (Exception e) {
            addError("Erreur: " + e.getMessage());
        }
        return null;
    }

    public void cancel() { selected = null; }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    public List<Event> getEvents() { return events; }
    public List<EventType> getEventTypes() { return eventTypes; }
    public Event getSelected() { return selected; }
    public void setSelected(Event selected) { this.selected = selected; }
    public boolean isEditMode() { return editMode; }
    public String getSelectedEventTypeID() { return selectedEventTypeID; }
    public void setSelectedEventTypeID(String s) { this.selectedEventTypeID = s; }
}