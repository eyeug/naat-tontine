package sn.naat.service;

import sn.naat.model.Enums.Espace;

import java.util.ArrayList;
import java.util.List;

/**
 * Règles des espaces (un écran par rôle) auxquels un compte a accès.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class EspacesRegles {

    private EspacesRegles() { }

    /**
     * Espaces d'un compte, du plus élevé au moins élevé (le premier est celui de l'arrivée).
     * Un gérant est toujours aussi membre (Membre &lt;|-- Gerant) : il a donc accès à l'espace membre,
     * qui contient les actions de membre (cotiser en ligne, demander un prêt, solde et distribution).
     */
    public static List<Espace> espaces(boolean administrateur, boolean gerant, boolean membre) {
        List<Espace> espaces = new ArrayList<>();
        if (administrateur) espaces.add(Espace.ADMINISTRATEUR);
        if (gerant) espaces.add(Espace.GERANT);
        if (membre || gerant) espaces.add(Espace.MEMBRE);
        return espaces;
    }

    /** Les autres espaces du compte, ceux vers lesquels « Changer d'espace » peut mener. */
    public static List<Espace> autres(List<Espace> espaces, Espace courant) {
        return espaces.stream().filter(e -> e != courant).toList();
    }

    /** Le bouton « Changer d'espace » n'a de sens que si le compte a au moins deux espaces. */
    public static boolean boutonVisible(List<Espace> espaces) {
        return espaces.size() > 1;
    }
}
