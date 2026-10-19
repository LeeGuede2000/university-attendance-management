package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Period;
import liten.attendance.domain.Term;
import liten.attendance.domain.TermPeriod;
import liten.attendance.domain.TermPeriodPK;
import liten.attendance.util.DataManager;

public class TermPeriodDAO {

    public List<TermPeriod> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT tp FROM TermPeriod tp ORDER BY tp.startDate DESC",
                    TermPeriod.class).getResultList();
        } finally {
            em.close();
        }
    }

    public TermPeriod findById(TermPeriodPK id) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(TermPeriod.class, id);
        } finally {
            em.close();
        }
    }

    public void save(TermPeriod tp) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(tp);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(TermPeriod tp) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(tp);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(TermPeriodPK id) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            TermPeriod tp = em.find(TermPeriod.class, id);
            if (tp != null) em.remove(tp);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Term> findAllTerms() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT t FROM Term t ORDER BY t.termID", Term.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Period> findAllPeriods() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Period p ORDER BY p.startYear", Period.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}