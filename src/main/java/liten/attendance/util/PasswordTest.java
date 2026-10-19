package liten.attendance.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Outil de diagnostic pour BCrypt.
 * À exécuter en cliquant droit dessus → Run File.
 * Affiche un nouveau hash valide de "Password1!" et teste
 * l'ancien hash qui était dans les données SQL.
 */
public class PasswordTest {

    public static void main(String[] args) {
        String motDePasse = "Password1!";

        System.out.println("=================================================");
        System.out.println("DIAGNOSTIC BCRYPT");
        System.out.println("=================================================");
        System.out.println("Mot de passe testé : " + motDePasse);
        System.out.println();

        // 1. Génère un NOUVEAU hash avec ta librairie BCrypt
        String nouveauHash = BCrypt.hashpw(motDePasse, BCrypt.gensalt(10));
        System.out.println("NOUVEAU HASH généré :");
        System.out.println(nouveauHash);
        System.out.println("Longueur : " + nouveauHash.length() + " (devrait être 60)");
        System.out.println();

        // 2. Vérifie que le nouveau hash correspond bien
        boolean nouveauOK = BCrypt.checkpw(motDePasse, nouveauHash);
        System.out.println("Vérification nouveau hash : " + (nouveauOK ? "OK ✓" : "ÉCHEC ✗"));
        System.out.println();

        // 3. Teste l'ancien hash (celui actuellement en base)
        String ancienHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        boolean ancienOK = BCrypt.checkpw(motDePasse, ancienHash);
        System.out.println("ANCIEN HASH (en base actuellement) :");
        System.out.println(ancienHash);
        System.out.println("Correspond à 'Password1!' ? " + (ancienOK ? "OUI ✓" : "NON ✗ (c'est la cause du bug)"));
        System.out.println();

        System.out.println("=================================================");
        System.out.println("CONCLUSION");
        System.out.println("=================================================");
        if (ancienOK) {
            System.out.println("L'ancien hash marche, bug ailleurs.");
        } else {
            System.out.println("L'ancien hash est CASSÉ.");
            System.out.println("Pour corriger : exécuter dans MySQL Workbench :");
            System.out.println();
            System.out.println("UPDATE account SET password = '" + nouveauHash + "';");
            System.out.println();
            System.out.println("(puis se connecter à nouveau avec admin / Password1!)");
        }
        System.out.println("=================================================");
    }
}