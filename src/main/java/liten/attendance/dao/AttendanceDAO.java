package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Attendance;
import liten.attendance.domain.StudentAttendance;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des présences (tables `attendance` et `studentattendance`).
 *
 * @author Liten
 */
public class AttendanceDAO {

    public Attendance findById(String attendanceID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Attendance.class, attendanceID);
        } finally {
            em.close();
        }
    }

    /** Sessions de présence d'un cours, triées par date décroissante. */
    public List<Attendance> findByCourse(String courseID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Attendance a "
                    + "WHERE a.course.courseID = :cid "
                    + "ORDER BY a.attendanceDate DESC",
                    Attendance.class)
                    .setParameter("cid", courseID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Toutes les présences/absences individuelles d'un étudiant. */
    public List<StudentAttendance> findByStudent(String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT sa FROM StudentAttendance sa "
                    + "WHERE sa.student.studentID = :sid "
                    + "ORDER BY sa.attendance.attendanceDate DESC",
                    StudentAttendance.class)
                    .setParameter("sid", studentID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Liste des absences (isPresent=false) d'un étudiant pour un cours donné. */
    public List<StudentAttendance> findAbsencesByStudentAndCourse(String studentID, String courseID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT sa FROM StudentAttendance sa "
                    + "WHERE sa.student.studentID = :sid "
                    + "AND sa.attendance.course.courseID = :cid "
                    + "AND sa.isPresent = false "
                    + "ORDER BY sa.attendance.attendanceDate DESC",
                    StudentAttendance.class)
                    .setParameter("sid", studentID)
                    .setParameter("cid", courseID)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Compte les absences d'un étudiant dans un cours. */
    public long countAbsences(String studentID, String courseID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(sa) FROM StudentAttendance sa "
                    + "WHERE sa.student.studentID = :sid "
                    + "AND sa.attendance.course.courseID = :cid "
                    + "AND sa.isPresent = false",
                    Long.class)
                    .setParameter("sid", studentID)
                    .setParameter("cid", courseID)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /** Sauvegarde une nouvelle session de présence avec ses lignes individuelles. */
    public void saveAttendanceSession(Attendance attendance, List<StudentAttendance> studentAttendances) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(attendance);
            for (StudentAttendance sa : studentAttendances) {
                em.persist(sa);
            }
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