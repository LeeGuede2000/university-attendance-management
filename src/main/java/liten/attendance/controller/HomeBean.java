package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import liten.attendance.dao.StudentDAO;

/**
 * Backing bean de la page d'accueil.
 * Récupère des statistiques simples depuis la base pour les afficher.
 *
 * @author Liten
 */
@Named("homeBean")
@RequestScoped
public class HomeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StudentDAO studentDAO = new StudentDAO();

    private long studentCount;

    /**
     * Méthode appelée automatiquement juste après la création du bean.
     * On en profite pour charger le nombre d'étudiants.
     */
    @PostConstruct
    public void init() {
        this.studentCount = studentDAO.countAll();
    }

    public long getStudentCount() {
        return studentCount;
    }
}