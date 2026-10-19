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
import liten.attendance.dao.RulesDAO;
import liten.attendance.domain.Rules;
import liten.attendance.domain.RulesType;
import liten.attendance.util.DataManager;

@Named("rulesBean")
@SessionScoped
public class RulesBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RulesDAO rulesDAO = new RulesDAO();

    private List<Rules> rules = new ArrayList<>();
    private List<RulesType> rulesTypes = new ArrayList<>();
    private Rules selected;
    private boolean editMode;
    private String selectedRulesTypeId;

    @PostConstruct
    public void init() {
        loadRules();
        loadRulesTypes();
    }

    private void loadRules() {
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<Rules> result = em.createNativeQuery(
                "SELECT * FROM rules WHERE isActive = 1 OR isActive IS NULL ORDER BY name",
                Rules.class).getResultList();
        rules = new ArrayList<>(result);
    } finally {
        em.close();
    }
}

    private void loadRulesTypes() {
        EntityManager em = DataManager.getEntityManager();
        try {
            rulesTypes = em.createQuery(
                    "SELECT r FROM RulesType r ORDER BY r.name", RulesType.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void prepareAdd() {
        selected = new Rules();
        selected.setIsActive(true);
        selected.setCreatedDate(LocalDate.now());
        selectedRulesTypeId = null;
        editMode = false;
    }

    public void prepareEdit(Rules rule) {
        selected = rule;
        selectedRulesTypeId = (rule.getRulesType() != null) ? rule.getRulesType().getRulesTypeID() : null;
        editMode = true;
    }

    public String save() {
        if (selected.getName() == null || selected.getName().isBlank()) {
            addError("Le nom est obligatoire.");
            return null;
        }
        try {
            // Résoudre l'objet RulesType depuis l'ID sélectionné
            if (selectedRulesTypeId != null && !selectedRulesTypeId.isBlank()) {
                EntityManager em = DataManager.getEntityManager();
                try {
                    RulesType rt = em.find(RulesType.class, selectedRulesTypeId);
                    selected.setRulesType(rt);
                } finally {
                    em.close();
                }
            } else {
                selected.setRulesType(null);
            }

            if (editMode) {
                rulesDAO.update(selected);
                addInfo("Règle mise à jour.");
            } else {
                selected.setRulesID("RUL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                rulesDAO.save(selected);
                addInfo("Règle créée.");
            }
            loadRules();
            selected = null;
            selectedRulesTypeId = null;
        } catch (Exception e) {
            addError("Erreur: " + e.getMessage());
        }
        return null;
    }

   public String delete(Rules rule) {
    try {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("UPDATE rules SET isActive = 0 WHERE rulesID = ?")
              .setParameter(1, rule.getRulesID())
              .executeUpdate();
            em.getTransaction().commit();
        } finally { em.close(); }
        addInfo("Règle désactivée (les données sont conservées).");
        loadRules();
    } catch (Exception e) {
        addError("Erreur: " + e.getMessage());
    }
    return null;
}

    public void cancel() {
        selected = null;
        selectedRulesTypeId = null;
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    public List<Rules> getRules() { return rules; }
    public List<RulesType> getRulesTypes() { return rulesTypes; }
    public Rules getSelected() { return selected; }
    public void setSelected(Rules s) { this.selected = s; }
    public boolean isEditMode() { return editMode; }
    public String getSelectedRulesTypeId() { return selectedRulesTypeId; }
    public void setSelectedRulesTypeId(String s) { this.selectedRulesTypeId = s; }
}