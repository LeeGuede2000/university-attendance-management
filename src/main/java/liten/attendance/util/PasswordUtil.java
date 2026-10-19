package liten.attendance.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire pour le hashage et la vérification des mots de passe.
 *
 * Utilise BCrypt, un algorithme conçu pour résister aux attaques par
 * force brute (lent par design, salt automatique).
 *
 * RÈGLE D'OR : on ne stocke JAMAIS un mot de passe en clair.
 * On stocke uniquement le hash, et on vérifie avec verify().
 *
 * @author Liten
 */
public final class PasswordUtil {

    /** Coût BCrypt : 10 = équilibre sécurité/performance correct. */
    private static final int BCRYPT_COST = 10;

    // Constructeur privé : classe utilitaire, pas d'instanciation
    private PasswordUtil() {
    }

    /**
     * Hash un mot de passe en clair avec BCrypt.
     * Le hash résultant inclut un salt aléatoire, donc deux appels
     * avec le même mot de passe produiront des hashs différents
     * (mais tous valides à la vérification).
     *
     * @param plainPassword le mot de passe en clair
     * @return le hash BCrypt (60 caractères)
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Vérifie qu'un mot de passe en clair correspond à un hash stocké.
     *
     * @param plainPassword le mot de passe saisi par l'utilisateur
     * @param hashedPassword le hash stocké en base
     * @return true si le mot de passe est correct
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Hash mal formé : on refuse plutôt que de planter
            return false;
        }
    }
}