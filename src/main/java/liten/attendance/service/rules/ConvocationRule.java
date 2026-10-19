package liten.attendance.service.rules;

import java.time.LocalDateTime;
import java.util.UUID;
import liten.attendance.dao.NotificationDAO;
import liten.attendance.domain.Course;
import liten.attendance.domain.Notification;
import liten.attendance.domain.Student;

/**
 * Règle 2 : 4 absences = convocation à la scolarité.
 */
public class ConvocationRule implements AbsenceRule {

    private static final int THRESHOLD = 4;

    @Override
    public boolean appliesTo(int absenceCount) {
        return absenceCount == THRESHOLD;
    }

    @Override
    public int getThreshold() {
        return THRESHOLD;
    }

    @Override
    public Notification execute(Student student, Course course, int absenceCount,
                                NotificationDAO notificationDAO) {

        Notification n = new Notification();
        n.setNotificationID("NOT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        n.setStudent(student);
        n.setCourse(course);
        n.setNotifType("CONVOCATION");
        n.setRecipientID("scolarite@iugb.ci");
        n.setChannel("Email");
        n.setSubject("[IUGB] Convocation - 4 absences atteintes");
        n.setMessage(String.format(
                "Étudiant à convoquer pour entretien obligatoire :\n\n"
                + "Nom : %s %s\nMatricule : %s\nÉcole : %s\n"
                + "Cours : %s (%s)\n"
                + "Absences non justifiées : %d\n\n"
                + "Fixer un rendez-vous dans la semaine.",
                student.getFirstName(), student.getLastName(),
                student.getMatricule(), student.getSchool(),
                course.getCode(), course.getName(), absenceCount));
        n.setIsRead(false);
        n.setIsSent(true);
        n.setSentDate(LocalDateTime.now());
        n.setCreatedDate(LocalDateTime.now());

        notificationDAO.save(n);
        return n;
    }
}