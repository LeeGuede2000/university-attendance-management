package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Event;
import liten.attendance.domain.EventType;
import liten.attendance.util.DataManager;

public class EventDAO {

    public List<Event> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                "SELECT e FROM Event e LEFT JOIN FETCH e.eventType ORDER BY e.createdDate DESC",
                Event.class
            ).getResultList();
        } finally {
            em.close();
        }
    }

    public Event findById(String eventID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Event.class, eventID);
        } finally {
            em.close();
        }
    }

    public void save(Event event) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(event);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Event event) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(event);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(String eventID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            Event event = em.find(Event.class, eventID);
            if (event != null) {
                em.remove(event);
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<EventType> findAllEventTypes() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                "SELECT et FROM EventType et ORDER BY et.name",
                EventType.class
            ).getResultList();
        } finally {
            em.close();
        }
    }
}