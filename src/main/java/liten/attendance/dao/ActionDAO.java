package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Action;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des actions (interventions des advisors).
 */
public class ActionDAO {

    public List<Action> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Action a ORDER BY a.actionDate DESC", Action.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Action findById(String actionID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Action.class, actionID);
        } finally {
            em.close();
        }
    }

    public List<Action> findByAdvisor(String advisorID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Action a WHERE a.advisor.facultyID = :aid "
                    + "ORDER BY a.actionDate DESC", Action.class)
                    .setParameter("aid", advisorID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Action> findByStudent(String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Action a WHERE a.student.studentID = :sid "
                    + "ORDER BY a.actionDate DESC", Action.class)
                    .setParameter("sid", studentID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(Action action) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(action);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Action action) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(action);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(String actionID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            Action a = em.find(Action.class, actionID);
            if (a != null) em.remove(a);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}