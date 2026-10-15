package sn.naat.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class Utils {

    private Utils() { }

    public static String nouvelId() {
        return UUID.randomUUID().toString();
    }

    /** Hachage SHA-256 simple du mot de passe (à remplacer par BCrypt en production). */
    public static String hasher(String motDePasseClair) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(motDePasseClair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Sans 0/O, 1/l/I, i/o : un mot de passe provisoire se dicte souvent au téléphone.
    private static final String ALPHABET_PROVISOIRE = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEA = new SecureRandom();

    /** Mot de passe provisoire de 8 caractères, tiré avec un générateur aléatoire cryptographique. */
    public static String genererMotDePasseProvisoire() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET_PROVISOIRE.charAt(ALEA.nextInt(ALPHABET_PROVISOIRE.length())));
        }
        return sb.toString();
    }

    public static boolean verifierMotDePasse(String motDePasseClair, String hash) {
        return hasher(motDePasseClair).equals(hash);
    }
}
