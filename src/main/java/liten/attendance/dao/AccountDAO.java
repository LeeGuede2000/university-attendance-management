package liten.attendance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.time.LocalDateTime;
import liten.attendance.domain.Account;
import liten.attendance.util.DataManager;

/**
 * Accès aux données des comptes utilisateurs (table `account`).
 *
 * @author Liten
 */
public class AccountDAO {

    /**
     * Recherche un compte par son nom d'utilisateur.
     * On utilise un paramètre nommé (:username) pour éviter
     * toute injection SQL.
     *
     * @param userName le nom d'utilisateur saisi
     * @return le compte trouvé, ou null s'il n'existe pas
     */
    public Account findByUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            return null;
        }
        EntityManager em = DataManager.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Account a WHERE a.userName = :username",
                    Account.class)
                    .setParameter("username", userName.trim())
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * Met à jour la date de dernière connexion et remet
     * à zéro le compteur d'échecs après un login réussi.
     *
     * @param accountID l'identifiant du compte connecté
     */
    public void recordSuccessfulLogin(String accountID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            Account account = em.find(Account.class, accountID);
            if (account != null) {
                account.setLastLoginDate(LocalDateTime.now());
                account.setFailedAttempts(0);
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Incrémente le compteur d'échecs après un login raté.
     * Utile pour détecter les tentatives d'attaque.
     *
     * @param accountID l'identifiant du compte concerné
     */
    public void recordFailedLogin(String accountID) {
        EntityManager em = DataManager.getEntityManager();
        try {
            em.getTransaction().begin();
            Account account = em.find(Account.class, accountID);
            if (account != null) {
                int attempts = account.getFailedAttempts() == null ? 0 : account.getFailedAttempts();
                account.setFailedAttempts(attempts + 1);
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}