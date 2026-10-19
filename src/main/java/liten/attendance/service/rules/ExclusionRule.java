package liten.attendance.service.rules;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import liten.attendance.dao.NotificationDAO;
import liten.attendance.domain.Course;
import liten.attendance.domain.Notification;
import liten.attendance.domain.Student;
import liten.attendance.util.DataManager;

/**
 * Règle 3 : 5+ absences = exclusion automatique.
 */
public class ExclusionRule implements AbsenceRule {

    private static final int THRESHOLD = 5;

    @Override
    public boolean appliesTo(int absenceCount) {
        return absenceCount >= THRESHOLD;
    }

    @Override
    public int getThreshold() {
        return THRESHOLD;
    }

    @Override
    public Notification execute(Student student, Course course, int absenceCount,
                                NotificationDAO notificationDAO) {

        // Exclure l'étudiant du cours
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "UPDATE enrolledcoursegrade SET isExcluded = 1 "
                    + "WHERE studentID = ? AND courseID = ?")
                    .setParameter(1, student.getStudentID())
                    .setParameter(2, course.getCourseID())
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }

        Notification n = new Notification();
        n.setNotificationID("NOT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        n.setStudent(student);
        n.setCourse(course);
        n.setNotifType("EXCLUSION");
        n.setRecipientID("scolarite@iugb.ci,direction@iugb.ci");
        n.setChannel("Email");
        n.setSubject("[IUGB] EXCLUSION - 5 absences atteintes");
        n.setMessage(String.format(
                "EXCLUSION AUTOMATIQUE\n\n"
                + "Étudiant : %s %s (%s)\nÉcole : %s\n"
                + "Cours : %s (%s)\n"
                + "Absences non justifiées : %d\n\n"
                + "Étudiant marqué comme exclu du cours.\n"
                + "Procédure officielle à enclencher.",
                student.getFirstName(), student.getLastName(), student.getMatricule(),
                student.getSchool(), course.getCode(), course.getName(), absenceCount));
        n.setIsRead(false);
        n.setIsSent(true);
        n.setSentDate(LocalDateTime.now());
        n.setCreatedDate(LocalDateTime.now());

        notificationDAO.save(n);
        return n;
    }
}