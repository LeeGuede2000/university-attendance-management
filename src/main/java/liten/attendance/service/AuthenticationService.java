package liten.attendance.service;

import liten.attendance.dao.AccountDAO;
import liten.attendance.domain.Account;
import liten.attendance.util.PasswordUtil;

/**
 * Service d'authentification.
 * Vérifie les identifiants et orchestre la logique de login.
 *
 * @author Liten
 */
public class AuthenticationService {

    private final AccountDAO accountDAO = new AccountDAO();

    /**
     * Tente de connecter un utilisateur avec ses identifiants.
     */
    public AuthResult login(String userName, String plainPassword) {
        // 1. Validation des entrées
        if (userName == null || userName.isBlank()
                || plainPassword == null || plainPassword.isBlank()) {
            return AuthResult.failure("Veuillez saisir un identifiant et un mot de passe");
        }

        // 2. Recherche du compte
        Account account = accountDAO.findByUserName(userName);
        if (account == null) {
            return AuthResult.failure("Identifiant ou mot de passe incorrect");
        }

        // 3. Vérification que le compte est actif
        if (Boolean.FALSE.equals(account.getIsActive())) {
            return AuthResult.failure("Ce compte a été désactivé. Contactez l'administrateur.");
        }

        // 4. Vérification du statut
        String status = account.getStatus();
        if (status != null && !status.equalsIgnoreCase("Approved")) {
            return AuthResult.failure("Ce compte n'est pas encore approuvé (statut : " + status + ")");
        }

        // 5. Vérification du mot de passe BCrypt
        if (!PasswordUtil.verify(plainPassword, account.getPassword())) {
            accountDAO.recordFailedLogin(account.getAccountID());
            return AuthResult.failure("Identifiant ou mot de passe incorrect");
        }

        // 6. Tout est bon : on enregistre la connexion réussie
        accountDAO.recordSuccessfulLogin(account.getAccountID());
        return AuthResult.success(account);
    }

    /**
     * Résultat d'une tentative d'authentification.
     */
    public static class AuthResult {

        private final boolean success;
        private final String message;
        private final Account account;

        private AuthResult(boolean success, String message, Account account) {
            this.success = success;
            this.message = message;
            this.account = account;
        }

        public static AuthResult success(Account account) {
            return new AuthResult(true, "Connexion réussie", account);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Account getAccount() { return account; }
    }
}