package liten.attendance.util;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import liten.attendance.domain.Account;

/**
 * Gestionnaire de session de l'utilisateur connecté.
 *
 * Annotation @SessionScoped : une instance est créée par session
 * HTTP (un par utilisateur connecté). L'instance persiste tant que
 * l'utilisateur navigue sur le site.
 *
 * Annotation @Named : permet d'accéder à ce bean depuis les pages JSF
 * via #{sessionManager.xxx}
 *
 * @author Liten
 */
@Named("sessionManager")
@SessionScoped
public class SessionManager implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Le compte de l'utilisateur connecté, ou null si non connecté. */
    private Account currentAccount;

    /**
     * Enregistre l'utilisateur après une connexion réussie.
     */
    public void login(Account account) {
        this.currentAccount = account;
    }

    /**
     * Déconnecte l'utilisateur actuel.
     */
    public void logout() {
        this.currentAccount = null;
    }

    /**
     * @return true si un utilisateur est connecté
     */
    public boolean isLoggedIn() {
        return currentAccount != null;
    }

    /**
     * @return le compte de l'utilisateur connecté, ou null
     */
    public Account getCurrentAccount() {
        return currentAccount;
    }

    /**
     * Raccourci pour obtenir le username de l'utilisateur connecté.
     * @return le username ou "Invité" si personne n'est connecté
     */
    public String getCurrentUserName() {
        return currentAccount != null ? currentAccount.getUserName() : "Invité";
    }

    /**
     * @return le nom du type de client (Student, Parent, Faculty...)
     *         ou null si pas connecté ou si pas de client associé
     */
    public String getCurrentClientTypeName() {
        if (currentAccount == null || currentAccount.getClient() == null
                || currentAccount.getClient().getClientType() == null) {
            return null;
        }
        return currentAccount.getClient().getClientType().getName();
    }

    /**
     * @return le nom du type de compte (Admin, Client, Employee)
     *         ou null si pas connecté
     */
    public String getCurrentAccountTypeName() {
        if (currentAccount == null || currentAccount.getAccountType() == null) {
            return null;
        }
        return currentAccount.getAccountType().getName();
    }

    /**
     * Vérifie si l'utilisateur connecté est administrateur.
     */
    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(getCurrentAccountTypeName());
    }
}