package liten.attendance.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Gère la connexion à la base de données via JPA.
 *
 * L'EntityManagerFactory est coûteux à créer : on n'en crée qu'UN seul
 * pour toute l'application (pattern Singleton).
 *
 * En revanche, chaque opération obtient son PROPRE EntityManager
 * (via getEntityManager()), ce qui rend le tout thread-safe.
 *
 * @author Liten
 */
public final class DataManager {

    /** Nom de l'unité de persistance défini dans persistence.xml */
    private static final String PERSISTENCE_UNIT = "attendancePU";

    /** Factory unique, partagée par toute l'application */
    private static EntityManagerFactory emf;

    // Constructeur privé : personne ne doit instancier cette classe
    private DataManager() {
    }

    /**
     * Retourne la factory unique, en la créant au premier appel.
     */
    private static synchronized EntityManagerFactory getFactory() {
        if (emf == null || !emf.isOpen()) {
            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        }
        return emf;
    }

    /**
     * Fournit un EntityManager neuf pour une opération.
     * IMPORTANT : l'appelant doit fermer cet EntityManager après usage
     * (idéalement dans un bloc try-finally).
     */
    public static EntityManager getEntityManager() {
        return getFactory().createEntityManager();
    }

    /**
     * Ferme proprement la factory (à appeler à l'arrêt de l'application).
     */
    public static void closeFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}