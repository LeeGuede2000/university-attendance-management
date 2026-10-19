package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.AbsenceJustification;
import liten.attendance.domain.AbsenceJustificationPK;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des justifications d'absence.
 */
public class JustificationDAO {

    public AbsenceJustification findById(String studentID, String attendanceID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(AbsenceJustification.class,
                    new AbsenceJustificationPK(studentID, attendanceID));
        } finally {
            em.close();
        }
    }

    public List<AbsenceJustification> findByStudent(String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT j FROM AbsenceJustification j "
                    + "WHERE j.student.studentID = :sid "
                    + "ORDER BY j.attendance.attendanceDate DESC",
                    AbsenceJustification.class)
                    .setParameter("sid", studentID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

   /** Justifs en attente d'examen advisor (Submitted). */
public List<AbsenceJustification> findPendingForAdvisor(String advisorID) {
    EntityManager em = DataManager.getEntityManager();
    try {
        return em.createQuery(
                "SELECT j FROM AbsenceJustification j "
                + "WHERE j.advisor.facultyID = :aid "
                + "AND j.workflowStatus = 'Submitted' "
                + "ORDER BY j.attendance.attendanceDate DESC",
                AbsenceJustification.class)
                .setParameter("aid", advisorID)
                .getResultList();
    } finally {
        em.close();
    }
}
/** Justifs en attente de décision finale par le prof. */
public List<AbsenceJustification> findPendingForFaculty(String facultyID) {
    EntityManager em = DataManager.getEntityManager();
    try {
        return em.createQuery(
                "SELECT j FROM AbsenceJustification j "
                + "WHERE j.faculty.facultyID = :fid "
                + "AND j.workflowStatus = 'AdvisorApproved' "
                + "ORDER BY j.attendance.attendanceDate DESC",
                AbsenceJustification.class)
                .setParameter("fid", facultyID)
                .getResultList();
    } finally {
        em.close();
    }
}

    public void save(AbsenceJustification justification) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(justification);
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

    public void update(AbsenceJustification justification) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(justification);
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