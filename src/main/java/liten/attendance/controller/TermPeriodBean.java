package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import liten.attendance.dao.TermPeriodDAO;
import liten.attendance.domain.Period;
import liten.attendance.domain.Term;
import liten.attendance.domain.TermPeriod;
import liten.attendance.domain.TermPeriodPK;
import liten.attendance.util.DataManager;

@Named("termPeriodBean")
@SessionScoped
public class TermPeriodBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TermPeriodDAO dao = new TermPeriodDAO();

    private List<TermPeriod> termPeriods = new ArrayList<>();
    private List<Term> terms = new ArrayList<>();
    private List<Period> periods = new ArrayList<>();

    private final Map<String, Term> termsById = new HashMap<>();
    private final Map<String, Period> periodsById = new HashMap<>();

    private TermPeriod selected;
    private boolean editMode = false;

    private String selectedTermID;
    private String selectedPeriodID;

    @PostConstruct
    public void init() {
        loadAll();
    }

    private void loadAll() {
        loadTermPeriods();
        terms = new ArrayList<>(dao.findAllTerms());
        periods = new ArrayList<>(dao.findAllPeriods());

        termsById.clear();
        periodsById.clear();
        for (Term t : terms) termsById.put(t.getTermID(), t);
        for (Period p : periods) periodsById.put(p.getPeriodID(), p);
    }

   private void loadTermPeriods() {
    EntityManager em = DataManager.getEntityManager();
    try {
        @SuppressWarnings("unchecked")
        List<TermPeriod> result = em.createNativeQuery(
                "SELECT * FROM termperiod WHERE isActive = 1 OR isActive IS NULL",
                TermPeriod.class).getResultList();
        termPeriods = new ArrayList<>(result);
    } finally {
        em.close();
    }
}

    public void prepareAdd() {
        selected = new TermPeriod();
        selectedTermID = null;
        selectedPeriodID = null;
        editMode = false;
    }

    public void prepareEdit(TermPeriod tp) {
        selected = tp;
        selectedTermID = tp.getId().getTermID();
        selectedPeriodID = tp.getId().getPeriodID();
        editMode = true;
    }

    public String save() {
        if (selectedTermID == null || selectedTermID.isBlank()) {
            addError("Le terme est obligatoire.");
            return null;
        }
        if (selectedPeriodID == null || selectedPeriodID.isBlank()) {
            addError("La période est obligatoire.");
            return null;
        }
        if (selected.getStartDate() == null || selected.getEndDate() == null) {
            addError("Les dates de début et de fin sont obligatoires.");
            return null;
        }
        if (selected.getEndDate().isBefore(selected.getStartDate())) {
            addError("La date de fin doit être postérieure à la date de début.");
            return null;
        }

        try {
            if (editMode) {
                dao.update(selected);
                addInfo("TermPeriod mis à jour.");
            } else {
                TermPeriodPK pk = new TermPeriodPK(selectedTermID, selectedPeriodID);
                if (dao.findById(pk) != null) {
                    addError("Cette combinaison Term + Période existe déjà.");
                    return null;
                }
                selected.setId(pk);
                dao.save(selected);
                addInfo("TermPeriod créé.");
            }
            loadAll();
            selected = null;
        } catch (Exception e) {
            addError("Erreur : " + e.getMessage());
        }
        return null;
    }

    public String delete(TermPeriod tp) {
        try {
            EntityManager em = DataManager.getEntityManager();
            try {
                em.getTransaction().begin();
                em.createNativeQuery(
                        "UPDATE termperiod SET isActive = 0 WHERE termID = ? AND periodID = ?")
                        .setParameter(1, tp.getId().getTermID())
                        .setParameter(2, tp.getId().getPeriodID())
                        .executeUpdate();
                em.getTransaction().commit();
            } finally { em.close(); }
            addInfo("Term-Period désactivé (les statistiques sont conservées).");
            loadAll();
        } catch (Exception e) {
            addError("Erreur: " + e.getMessage());
        }
        return null;
    }

    public void cancel() {
        selected = null;
    }

    public String getTermName(TermPeriod tp) {
        if (tp == null || tp.getId() == null) return "—";
        Term t = termsById.get(tp.getId().getTermID());
        return t != null && t.getName() != null ? t.getName() : tp.getId().getTermID();
    }

    public String getTermShortName(TermPeriod tp) {
        if (tp == null || tp.getId() == null) return "";
        Term t = termsById.get(tp.getId().getTermID());
        return t != null && t.getShortName() != null ? t.getShortName() : "";
    }

    public String getPeriodLabel(TermPeriod tp) {
        if (tp == null || tp.getId() == null) return "—";
        Period p = periodsById.get(tp.getId().getPeriodID());
        if (p == null) return tp.getId().getPeriodID();
        return p.getStartYear() + "-" + p.getEndYear();
    }

    public long getDurationDays(TermPeriod tp) {
        if (tp == null || tp.getStartDate() == null || tp.getEndDate() == null) return 0;
        return tp.getEndDate().toEpochDay() - tp.getStartDate().toEpochDay();
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    public List<TermPeriod> getTermPeriods() { return termPeriods; }
    public List<Term> getTerms() { return terms; }
    public List<Period> getPeriods() { return periods; }
    public TermPeriod getSelected() { return selected; }
    public void setSelected(TermPeriod selected) { this.selected = selected; }
    public boolean isEditMode() { return editMode; }
    public String getSelectedTermID() { return selectedTermID; }
    public void setSelectedTermID(String s) { this.selectedTermID = s; }
    public String getSelectedPeriodID() { return selectedPeriodID; }
    public void setSelectedPeriodID(String s) { this.selectedPeriodID = s; }
}