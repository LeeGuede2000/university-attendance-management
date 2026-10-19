package liten.attendance.service.rules;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import liten.attendance.dao.NotificationDAO;
import liten.attendance.domain.Course;
import liten.attendance.domain.Notification;
import liten.attendance.domain.Student;
import liten.attendance.util.DataManager;

/**
 * Règle 1 : 3 absences = notification au(x) parent(s) par email.
 */
public class NotifyParentRule implements AbsenceRule {

    private static final int THRESHOLD = 3;

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

        EntityManager em = DataManager.getEntityManager();
        String parentEmail;
        String parentName = "votre enfant";
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT p.firstName, p.lastName, acc.userName "
                    + "FROM studentparent sp "
                    + "JOIN parent p ON p.parentID = sp.parentID "
                    + "LEFT JOIN client cl ON cl.personID = p.parentID "
                    + "LEFT JOIN account acc ON acc.clientID = cl.clientID "
                    + "WHERE sp.studentID = ? LIMIT 1")
                    .setParameter(1, student.getStudentID())
                    .getResultList();

            if (!rows.isEmpty()) {
                Object[] row = rows.get(0);
                parentName = row[0] + " " + row[1];
                String userName = (String) row[2];
                parentEmail = (userName != null && !userName.isBlank())
                        ? userName + "@parent.iugb.ci"
                        : "parent.unknown@iugb.ci";
            } else {
                parentEmail = "no.parent@iugb.ci";
            }
        } finally {
            em.close();
        }

        Notification n = new Notification();
        n.setNotificationID("NOT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        n.setStudent(student);
        n.setCourse(course);
        n.setNotifType("PARENT_ALERT");
        n.setRecipientID(parentEmail);
        n.setChannel("Email");
        n.setSubject("[IUGB] Alerte absences de votre enfant");
        n.setMessage(String.format(
                "Bonjour %s,\n\nVotre enfant %s %s a cumulé "
                + "%d absences non justifiées dans le cours %s (%s).\n\n"
                + "Nous vous invitons à prendre contact avec son advisor.\n\n"
                + "Cordialement,\nAdministration IUGB",
                parentName,
                student.getFirstName(), student.getLastName(),
                absenceCount, course.getCode(), course.getName()));
        n.setIsRead(false);
        n.setIsSent(true);
        n.setSentDate(LocalDateTime.now());
        n.setCreatedDate(LocalDateTime.now());

        notificationDAO.save(n);
        return n;
    }
}