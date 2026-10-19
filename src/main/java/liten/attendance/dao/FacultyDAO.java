package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Faculty;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des professeurs (table `faculty`).
 *
 * @author Liten
 */
public class FacultyDAO {

    public Faculty findById(String facultyID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Faculty.class, facultyID);
        } finally {
            em.close();
        }
    }

    public List<Faculty> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery("SELECT f FROM Faculty f ORDER BY f.lastName", Faculty.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    /** Tous les profs qui sont aussi advisors. */
    public List<Faculty> findAllAdvisors() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT f FROM Faculty f WHERE f.isAdvisor = true ORDER BY f.lastName",
                    Faculty.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}