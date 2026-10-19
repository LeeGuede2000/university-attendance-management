package liten.attendance.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import liten.attendance.service.AuthenticationService;
import liten.attendance.service.AuthenticationService.AuthResult;
import liten.attendance.util.SessionManager;

/**
 * Backing bean du formulaire de connexion.
 *
 * Redirige automatiquement vers le dashboard correspondant au rôle :
 *   - Admin → /admin/dashboard.xhtml
 *   - Employee (accountType) → /employee/dashboard.xhtml
 *   - Client/Student → /student/dashboard.xhtml
 *   - Client/Faculty → /faculty/dashboard.xhtml
 *   - Client/Parent → /parent/dashboard.xhtml
 *   - Client/Manager → /manager/dashboard.xhtml
 *   - Client/Employee → /employee/dashboard.xhtml
 *
 * @author Liten
 */
@Named("loginBean")
@RequestScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private AuthenticationService authService;

    @Inject
    private SessionManager sessionManager;

    private String userName;
    private String password;

    @PostConstruct
    public void init() {
        this.authService = new AuthenticationService();
    }

    /** Action appelée par le bouton "Se connecter". */
    public String doLogin() {
        AuthResult result = authService.login(userName, password);
        if (!result.isSuccess()) {
            addError(result.getMessage());
            return null;
        }
        sessionManager.login(result.getAccount());
        return redirectToDashboard();
    }

    /**
     * Détermine la page de redirection selon le rôle de l'utilisateur connecté.
     */
    private String redirectToDashboard() {
        if (sessionManager.getCurrentAccount() == null) {
            return "/welcome.xhtml?faces-redirect=true";
        }

        // 1. AccountType prioritaire (Admin, Employee, Client)
        String accountTypeName = null;
        if (sessionManager.getCurrentAccount().getAccountType() != null) {
            accountTypeName = sessionManager.getCurrentAccount().getAccountType().getName();
        }

        if ("Admin".equalsIgnoreCase(accountTypeName)) {
            return "/admin/dashboard.xhtml?faces-redirect=true";
        }

        if ("Employee".equalsIgnoreCase(accountTypeName)) {
            return "/employee/dashboard.xhtml?faces-redirect=true";
        }

        // 2. Si AccountType = Client, on regarde le ClientType
        String clientTypeName = null;
        if (sessionManager.getCurrentAccount().getClient() != null
                && sessionManager.getCurrentAccount().getClient().getClientType() != null) {
            clientTypeName = sessionManager.getCurrentAccount().getClient().getClientType().getName();
        }

        if (clientTypeName == null) {
            return "/welcome.xhtml?faces-redirect=true";
        }

        switch (clientTypeName.toLowerCase()) {
            case "student":
                return "/student/dashboard.xhtml?faces-redirect=true";
            case "faculty":
                return "/faculty/dashboard.xhtml?faces-redirect=true";
            case "parent":
                return "/parent/dashboard.xhtml?faces-redirect=true";
            case "manager":
                return "/manager/dashboard.xhtml?faces-redirect=true";
            case "employee":
                return "/employee/dashboard.xhtml?faces-redirect=true";
            default:
                return "/welcome.xhtml?faces-redirect=true";
        }
    }

    /** Déconnecte l'utilisateur. */
    public String doLogout() {
        sessionManager.logout();
        try {
            FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        } catch (Exception e) {
            // ignore
        }
        return "/login.xhtml?faces-redirect=true";
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}