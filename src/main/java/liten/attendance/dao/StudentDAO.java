package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import java.util.List;
import liten.attendance.domain.Student;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des étudiants (table `student`).
 * Chaque méthode ouvre son propre EntityManager et le ferme proprement.
 *
 * @author Liten
 */
public class StudentDAO {

    /**
     * Compte le nombre total d'étudiants dans la base.
     *
     * @return le nombre d'étudiants
     */
    public long countAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(s) FROM Student s", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Récupère tous les étudiants.
     *
     * @return la liste de tous les étudiants
     */
    public List<Student> findAll() {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery("SELECT s FROM Student s", Student.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Recherche un étudiant par son identifiant.
     *
     * @param studentID l'identifiant de l'étudiant
     * @return l'étudiant trouvé, ou null s'il n'existe pas
     */
    public Student findById(String studentID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.find(Student.class, studentID);
        } finally {
            em.close();
        }
    }
}