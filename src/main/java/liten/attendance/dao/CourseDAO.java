package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Course;
import liten.attendance.util.DataManager;

public class CourseDAO {

    public Course findById(String courseID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Course.class, courseID);
        } finally {
            em.close();
        }
    }

    public List<Course> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT c FROM Course c ORDER BY c.code",
                    Course.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Course> findCoursesByFaculty(String facultyID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<String> courseIds = em.createNativeQuery(
                    "SELECT DISTINCT courseID FROM assignment "
                    + "WHERE facultyID = ? AND isValid = 1")
                    .setParameter(1, facultyID)
                    .getResultList();

            if (courseIds == null || courseIds.isEmpty()) {
                return em.createQuery(
                        "SELECT c FROM Course c ORDER BY c.code",
                        Course.class)
                        .getResultList();
            }

            return em.createQuery(
                    "SELECT c FROM Course c WHERE c.courseID IN :ids ORDER BY c.code",
                    Course.class)
                    .setParameter("ids", courseIds)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(Course course) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(course);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Course course) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(course);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(String courseID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            Course c = em.find(Course.class, courseID);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}