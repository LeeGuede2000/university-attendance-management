package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import liten.attendance.dao.ActionDAO;
import liten.attendance.dao.FacultyDAO;
import liten.attendance.domain.Action;
import liten.attendance.domain.Course;
import liten.attendance.domain.Faculty;
import liten.attendance.domain.Student;
import liten.attendance.util.DataManager;
import liten.attendance.util.SessionManager;

@Named("actionBean")
@SessionScoped
public class ActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionManager sessionManager;

    private final ActionDAO actionDAO = new ActionDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();

    private Faculty currentAdvisor;
    private List<Action> actions = new ArrayList<>();
    private List<Student> availableStudents = new ArrayList<>();
    private List<Course> availableCourses = new ArrayList<>();

    private Action selected;
    private boolean editMode = false;

    // Champs sélection FK
    private String selectedStudentId;
    private String selectedCourseId;

    @PostConstruct
    public void init() {
        resolveCurrent();
        loadActions();
        loadStudents();
        loadCourses();
    }

    private void resolveCurrent() {
        if (sessionManager.getCurrentAccount() == null
                || sessionManager.getCurrentAccount().getClient() == null) return;
        String personID = sessionManager.getCurrentAccount().getClient().getPersonID();
        if (personID != null) {
            currentAdvisor = facultyDAO.findById(personID);
        }
    }

    private void loadActions() {
        if (currentAdvisor != null) {
            actions = actionDAO.findByAdvisor(currentAdvisor.getFacultyID());
        } else {
            actions = actionDAO.findAll();
        }
    }

    private void loadStudents() {
        EntityManager em = DataManager.getEntityManager();
        try {
            availableStudents = em.createQuery(
                    "SELECT s FROM Student s ORDER BY s.lastName, s.firstName", Student.class)
                    .setMaxResults(200)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private void loadCourses() {
        EntityManager em = DataManager.getEntityManager();
        try {
            availableCourses = em.createQuery(
                    "SELECT c FROM Course c ORDER BY c.code", Course.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void prepareAdd() {
        selected = new Action();
        selected.setActionDate(LocalDateTime.now());
        selected.setCreatedDate(LocalDateTime.now());
        selected.setIsCompleted(false);
        selected.setNbrAbsences(0);
        selectedStudentId = null;
        selectedCourseId = null;
        editMode = false;
    }

    public void prepareEdit(Action action) {
        selected = action;
        selectedStudentId = action.getStudent() != null ? action.getStudent().getStudentID() : null;
        selectedCourseId = action.getCourse() != null ? action.getCourse().getCourseID() : null;
        editMode = true;
    }

    public String save() {
        if (selected.getName() == null || selected.getName().isBlank()) {
            addError("Le nom de l'action est obligatoire.");
            return null;
        }

        EntityManager em = DataManager.getEntityManager();
        try {
            if (selectedStudentId != null && !selectedStudentId.isBlank()) {
                selected.setStudent(em.find(Student.class, selectedStudentId));
            }
            if (selectedCourseId != null && !selectedCourseId.isBlank()) {
                selected.setCourse(em.find(Course.class, selectedCourseId));
            }
            if (currentAdvisor != null && selected.getAdvisor() == null) {
                selected.setAdvisor(currentAdvisor);
            }
        } finally {
            em.close();
        }

        try {
            if (editMode) {
                actionDAO.update(selected);
                addInfo("Action mise à jour.");
            } else {
                selected.setActionID("ACT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                actionDAO.save(selected);
                addInfo("Action créée.");
            }
            loadActions();
            selected = null;
        } catch (Exception e) {
            addError("Erreur: " + e.getMessage());
        }
        return null;
    }

    public String delete(Action action) {
        try {
            actionDAO.delete(action.getActionID());
            addInfo("Action supprimée.");
            loadActions();
        } catch (Exception e) {
            addError("Erreur lors de la suppression: " + e.getMessage());
        }
        return null;
    }

    public String markCompleted(Action action) {
        try {
            action.setIsCompleted(true);
            actionDAO.update(action);
            addInfo("Action marquée comme terminée.");
            loadActions();
        } catch (Exception e) {
            addError("Erreur: " + e.getMessage());
        }
        return null;
    }

    public void cancel() {
        selected = null;
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    // Getters / Setters
    public Faculty getCurrentAdvisor() { return currentAdvisor; }
    public List<Action> getActions() { return actions; }
    public List<Student> getAvailableStudents() { return availableStudents; }
    public List<Course> getAvailableCourses() { return availableCourses; }
    public Action getSelected() { return selected; }
    public void setSelected(Action s) { this.selected = s; }
    public boolean isEditMode() { return editMode; }
    public String getSelectedStudentId() { return selectedStudentId; }
    public void setSelectedStudentId(String s) { this.selectedStudentId = s; }
    public String getSelectedCourseId() { return selectedCourseId; }
    public void setSelectedCourseId(String s) { this.selectedCourseId = s; }
}