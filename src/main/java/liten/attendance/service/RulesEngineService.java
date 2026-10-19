package liten.attendance.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import liten.attendance.dao.NotificationDAO;
import liten.attendance.domain.Course;
import liten.attendance.domain.Notification;
import liten.attendance.domain.Student;
import liten.attendance.service.rules.AbsenceRule;
import liten.attendance.service.rules.ConvocationRule;
import liten.attendance.service.rules.ExclusionRule;
import liten.attendance.service.rules.NotifyParentRule;
import liten.attendance.util.DataManager;

/**
 * Moteur de règles automatiques d'absences.
 *
 * À appeler après chaque création d'absence (ou prise de présence).
 * Compte les absences non justifiées de l'étudiant dans le cours,
 * et applique toutes les règles qui correspondent.
 *
 * @author Liten
 */
public class RulesEngineService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** Liste des règles configurées (ordre = ordre d'évaluation). */
    private final List<AbsenceRule> rules = Arrays.asList(
            new NotifyParentRule(),
            new ConvocationRule(),
            new ExclusionRule()
    );

    /**
     * Évalue toutes les règles pour un étudiant dans un cours donné.
     *
     * @return les notifications créées (utile pour les logs / debug)
     */
    public List<Notification> evaluateForStudent(Student student, Course course) {
        List<Notification> created = new ArrayList<>();

        int absenceCount = countUnjustifiedAbsences(student.getStudentID(),
                                                    course.getCourseID());

        for (AbsenceRule rule : rules) {
            if (rule.appliesTo(absenceCount)) {
                Notification n = rule.execute(student, course, absenceCount, notificationDAO);
                if (n != null) created.add(n);
            }
        }
        return created;
    }

    /**
     * Compte le nombre d'absences NON justifiées (workflowStatus != 'Approved')
     * d'un étudiant dans un cours donné.
     */
    private int countUnjustifiedAbsences(String studentID, String courseID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            // Nombre total d'absences (isPresent=false)
            Number total = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM studentattendance sa "
                    + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                    + "WHERE sa.studentID = ? AND a.courseID = ? AND sa.isPresent = 0")
                    .setParameter(1, studentID)
                    .setParameter(2, courseID)
                    .getSingleResult();

            // Nombre d'absences DÉJÀ approuvées (workflowStatus = 'Approved')
            Number approved = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM absencejustification j "
                    + "JOIN attendance a ON a.attendanceID = j.attendanceID "
                    + "WHERE j.studentID = ? AND a.courseID = ? "
                    + "AND j.workflowStatus = 'Approved'")
                    .setParameter(1, studentID)
                    .setParameter(2, courseID)
                    .getSingleResult();

            return total.intValue() - approved.intValue();
        } finally {
            em.close();
        }
    }
}