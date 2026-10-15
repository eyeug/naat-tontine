package sn.naat.util;

import sn.naat.model.Enums.Espace;
import sn.naat.model.Utilisateur;

import java.util.List;

/** Garde en mémoire l'utilisateur connecté pendant toute la session de l'application. */
public final class SessionContext {

    private static Utilisateur utilisateurConnecte;
    private static List<Espace> espaces = List.of();

    private SessionContext() { }

    public static void connecter(Utilisateur u) {
        utilisateurConnecte = u;
    }

    public static Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    /** Espaces (rôles) auxquels le compte a accès, calculés une fois à la connexion. */
    public static List<Espace> getEspaces() {
        return espaces;
    }

    public static void setEspaces(List<Espace> espacesDisponibles) {
        espaces = List.copyOf(espacesDisponibles);
    }

    public static void deconnecter() {
        utilisateurConnecte = null;
        espaces = List.of();
    }
}
