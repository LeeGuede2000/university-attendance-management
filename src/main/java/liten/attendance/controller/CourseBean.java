package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import liten.attendance.dao.CourseDAO;
import liten.attendance.domain.Course;
import liten.attendance.util.DataManager;

@Named("courseBean")
@SessionScoped
public class CourseBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final CourseDAO courseDAO = new CourseDAO();
    private List<Course> courses = new ArrayList<>();
    private Course selected;
    private boolean editMode = false;

    @PostConstruct
    public void init() {
        loadCourses();
    }

    private void loadCourses() {
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Course> result = em.createNativeQuery(
                "SELECT * FROM course WHERE isActive = 1 OR isActive IS NULL ORDER BY code",
                Course.class).getResultList();
        courses = new ArrayList<>(result);
    } finally {
        em.close();
    }
}
    public void prepareAdd() {
        selected = new Course();
        selected.setCreatedDate(LocalDate.now());
        editMode = false;
    }

    public void prepareEdit(Course course) {
        selected = course;
        editMode = true;
    }

    public String save() {
        if (selected.getName() == null || selected.getName().isBlank()) {
            addError("Le nom du cours est obligatoire.");
            return null;
        }
        if (selected.getCode() == null || selected.getCode().isBlank()) {
            addError("Le code du cours est obligatoire.");
            return null;
        }
        try {
            if (editMode) {
                courseDAO.update(selected);
                addInfo("Cours mis à jour.");
            } else {
                selected.setCourseID("CRS_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                courseDAO.save(selected);
                addInfo("Cours créé.");
            }
            loadCourses();
            selected = null;
        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        }
        return null;
    }

    public String delete(Course course) {
        try {
            EntityManager em = DataManager.getEntityManager();
            try {
                em.getTransaction().begin();
                em.createNativeQuery("UPDATE course SET isActive = 0 WHERE courseID = ?")
                  .setParameter(1, course.getCourseID())
                  .executeUpdate();
                em.getTransaction().commit();
            } finally { em.close(); }
            addInfo("Cours désactivé (les statistiques sont conservées).");
            loadCourses();
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

    public List<Course> getCourses() { return courses; }
    public Course getSelected() { return selected; }
    public void setSelected(Course selected) { this.selected = selected; }
    public boolean isEditMode() { return editMode; }
}