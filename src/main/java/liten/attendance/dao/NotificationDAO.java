package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Notification;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des notifications.
 */
public class NotificationDAO {

    public Notification findById(String notificationID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Notification.class, notificationID);
        } finally {
            em.close();
        }
    }

    public List<Notification> findByStudent(String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT n FROM Notification n "
                    + "WHERE n.student.studentID = :sid "
                    + "ORDER BY n.sentDate DESC",
                    Notification.class)
                    .setParameter("sid", studentID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Notification> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT n FROM Notification n ORDER BY n.sentDate DESC",
                    Notification.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(Notification notification) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(notification);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}