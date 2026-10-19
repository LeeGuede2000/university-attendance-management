package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("employeeDashboardBean")
@SessionScoped
public class EmployeeDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private String employeeName;
    private String employeeDirection;
    private String employeeID;

    private int totalEvents;
    private int totalEventTypes;
    private int totalStudents;
    private int totalAttendances;

    private List<EventRow> recentEvents = new ArrayList<>();
    private List<EventTypeStat> eventTypeStats = new ArrayList<>();
    private List<StudentRow> studentsList = new ArrayList<>();

    @PostConstruct
    public void init() {
        resolveCurrent();
        loadCounters();
        loadRecentEvents();
        loadEventTypeStats();
        loadStudentsList();
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null) return;

        EntityManager em = DataManager.getEntityManager();
        try {
            if (sessionManager.getCurrentAccount().getClient() != null) {
                employeeID = sessionManager.getCurrentAccount().getClient().getPersonID();
            }

            if (employeeID == null || employeeID.isBlank()) {
                String userName = sessionManager.getCurrentUserName();
                try {
                    Object[] r = (Object[]) em.createNativeQuery(
                            "SELECT e.employeeID, e.firstName, e.lastName, e.direction "
                            + "FROM employee e "
                            + "JOIN client c ON c.personID = e.employeeID "
                            + "JOIN account a ON a.clientID = c.clientID "
                            + "WHERE a.userName = ?")
                            .setParameter(1, userName)
                            .getSingleResult();
                    employeeID = (String) r[0];
                    employeeName = r[1] + " " + r[2];
                    employeeDirection = (String) r[3];
                    return;
                } catch (Exception ignored) {}
            }

            if (employeeID != null && !employeeID.isBlank()) {
                Object[] r = (Object[]) em.createNativeQuery(
                        "SELECT firstName, lastName, direction FROM employee WHERE employeeID = ?")
                        .setParameter(1, employeeID)
                        .getSingleResult();
                employeeName = r[0] + " " + r[1];
                employeeDirection = (String) r[2];
            }
        } catch (Exception e) {
            employeeName = sessionManager.getCurrentUserName();
            employeeDirection = "IUGB Staff";
        } finally {
            em.close();
        }
    }

    private void loadCounters() {
        EntityManager em = DataManager.getEntityManager();
        try {
            totalEvents = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM event").getSingleResult()).intValue();
            totalEventTypes = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM eventtype").getSingleResult()).intValue();
            totalStudents = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM student").getSingleResult()).intValue();
            totalAttendances = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM attendance").getSingleResult()).intValue();
        } catch (Exception e) {
            totalEvents = 0; totalEventTypes = 0; totalStudents = 0; totalAttendances = 0;
        } finally {
            em.close();
        }
    }

    private void loadRecentEvents() {
        recentEvents.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT e.eventID, e.name, e.description, e.createdDate, "
                    + "  et.name AS typeName, "
                    + "  s.eventDate, s.startHour, s.endHour, s.dayName "
                    + "FROM event e "
                    + "LEFT JOIN eventtype et ON et.eventTypeID = e.eventTypeID "
                    + "LEFT JOIN schedule s ON s.scheduleID = e.scheduleID "
                    + "ORDER BY e.createdDate DESC LIMIT 20")
                    .getResultList();

            for (Object[] r : rows) {
                String createdStr = r[3] != null
                        ? r[3].toString().substring(0, Math.min(10, r[3].toString().length())) : "";
                String eventDateStr = r[5] != null
                        ? r[5].toString().substring(0, Math.min(10, r[5].toString().length())) : "—";
                String startStr = r[6] != null
                        ? r[6].toString().substring(0, Math.min(5, r[6].toString().length())) : "";
                String endStr = r[7] != null
                        ? r[7].toString().substring(0, Math.min(5, r[7].toString().length())) : "";
                String schedule = (startStr.isEmpty() && endStr.isEmpty())
                        ? "—" : (startStr + " - " + endStr);

                recentEvents.add(new EventRow(
                        (String) r[0], (String) r[1],
                        r[2] != null ? (String) r[2] : "",
                        createdStr,
                        r[4] != null ? (String) r[4] : "—",
                        eventDateStr, schedule,
                        r[8] != null ? (String) r[8] : ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private void loadEventTypeStats() {
        eventTypeStats.clear();
        EntityManager em = DataManager.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT et.name, et.description, COUNT(e.eventID) AS nb "
                    + "FROM eventtype et "
                    + "LEFT JOIN event e ON e.eventTypeID = et.eventTypeID "
                    + "GROUP BY et.name, et.description ORDER BY nb DESC")
                    .getResultList();

            for (Object[] r : rows) {
                eventTypeStats.add(new EventTypeStat(
                        (String) r[0], (String) r[1],
                        r[2] != null ? ((Number) r[2]).intValue() : 0));
            }
        } catch (Exception e) {
            // silent
        } finally {
            em.close();
        }
    }

    private void loadStudentsList() {
    studentsList.clear();
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT DISTINCT s.studentID, s.firstName, s.lastName, s.matricule, "
                + "  s.school, s.academicLevel, COUNT(sa.attendanceID) AS nbParticipations "
                + "FROM student s "
                + "JOIN studentattendance sa ON sa.studentID = s.studentID "
                + "JOIN attendance a ON a.attendanceID = sa.attendanceID "
                + "WHERE sa.isPresent = 1 "
                + "  AND a.attendanceDate < CURDATE() "
                + "GROUP BY s.studentID, s.firstName, s.lastName, s.matricule, s.school, s.academicLevel "
                + "ORDER BY nbParticipations DESC LIMIT 50")
                .getResultList();

        for (Object[] r : rows) {
            int nb = r[6] != null ? ((Number) r[6]).intValue() : 0;
            studentsList.add(new StudentRow(
                    (String) r[0], (String) r[1], (String) r[2],
                    (String) r[3], (String) r[4], (String) r[5], nb));
        }
    } finally { em.close(); }
}

    public String refresh() {
        loadCounters();
        loadRecentEvents();
        loadEventTypeStats();
        loadStudentsList();
        return null;
    }

    public String getEmployeeName() { return employeeName; }
    public String getEmployeeDirection() { return employeeDirection; }
    public String getEmployeeID() { return employeeID; }
    public int getTotalEvents() { return totalEvents; }
    public int getTotalEventTypes() { return totalEventTypes; }
    public int getTotalStudents() { return totalStudents; }
    public int getTotalAttendances() { return totalAttendances; }
    public List<EventRow> getRecentEvents() { return recentEvents; }
    public List<EventTypeStat> getEventTypeStats() { return eventTypeStats; }
    public List<StudentRow> getStudentsList() { return studentsList; }

    public static class EventRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String eventID, name, description, createdDate, typeName, eventDate, schedule, dayName;
        public EventRow(String i, String n, String d, String cd, String tn, String ed, String sc, String dn) {
            this.eventID = i; this.name = n; this.description = d; this.createdDate = cd;
            this.typeName = tn; this.eventDate = ed; this.schedule = sc; this.dayName = dn;
        }
        public String getEventID() { return eventID; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCreatedDate() { return createdDate; }
        public String getTypeName() { return typeName; }
        public String getEventDate() { return eventDate; }
        public String getSchedule() { return schedule; }
        public String getDayName() { return dayName; }
        public String getColor() {
            switch (typeName != null ? typeName : "") {
                case "Course": return "#3b82f6";
                case "Exam": return "#dc2626";
                case "Conference": return "#8b5cf6";
                case "Workshop": return "#16a34a";
                case "Convocation": return "#f59e0b";
                case "Lab": return "#0891b2";
                default: return "#64748b";
            }
        }
    }

    public static class EventTypeStat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name, description;
        private final int nbEvents;
        public EventTypeStat(String n, String d, int nb) {
            this.name = n; this.description = d; this.nbEvents = nb;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getNbEvents() { return nbEvents; }
    }

   public static class StudentRow implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String studentID, firstName, lastName, matricule, school, level;
    private final int nbParticipations;
    public StudentRow(String i, String f, String l, String m, String s, String lv, int nb) {
        this.studentID = i; this.firstName = f; this.lastName = l;
        this.matricule = m; this.school = s; this.level = lv;
        this.nbParticipations = nb;
    }
    public String getStudentID() { return studentID; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getMatricule() { return matricule; }
    public String getSchool() { return school; }
    public String getLevel() { return level; }
    public int getNbParticipations() { return nbParticipations; }
}
}