package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Rules;
import liten.attendance.util.DataManager;

public class RulesDAO {

    public List<Rules> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM Rules r ORDER BY r.name", Rules.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Rules findById(String rulesID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Rules.class, rulesID);
        } finally {
            em.close();
        }
    }

    public void save(Rules rule) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(rule);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Rules rule) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(rule);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(String rulesID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            Rules r = em.find(Rules.class, rulesID);
            if (r != null) em.remove(r);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}