package liten.attendance.service.rules;

import liten.attendance.dao.NotificationDAO;
import liten.attendance.domain.Course;
import liten.attendance.domain.Notification;
import liten.attendance.domain.Student;

/**
 * Stratégie d'application d'une règle d'absences automatique.
 *
 * Chaque règle décide indépendamment si elle s'applique à un nombre
 * d'absences donné, et exécute son action si oui.
 *
 * @author Liten
 */
public interface AbsenceRule {

    /**
     * @param absenceCount nombre d'absences non justifiées de l'étudiant
     * @return true si la règle s'applique à ce nombre
     */
    boolean appliesTo(int absenceCount);

    /**
     * Exécute la règle : crée une notification, met à jour la BDD, etc.
     *
     * @param student        l'étudiant concerné
     * @param course         le cours concerné
     * @param absenceCount   nombre d'absences actuel
     * @param notificationDAO DAO pour persister la notification
     * @return la notification créée (peut être nulle si la règle ne crée pas de notif)
     */
    Notification execute(Student student, Course course, int absenceCount,
                         NotificationDAO notificationDAO);

    /** Nombre d'absences qui déclenche cette règle. */
    int getThreshold();
}