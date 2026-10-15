package sn.naat.service;

import java.util.Map;

/**
 * Affichage d'une personne dans un tableau : son nom, jamais son identifiant technique
 * (un UUID illisible comme « 33290c16-ee6a-4b56… »).
 *
 * Fonction pure (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class NomsRegles {

    private NomsRegles() { }

    /**
     * @param noms        table identifiant → nom, déjà chargée
     * @param id          identifiant de la personne ; peut être vide (action faite par le système)
     * @param siVide      texte affiché quand il n'y a pas d'identifiant (ex. « Système »)
     * @return le nom ; l'identifiant lui-même si le compte est inconnu (la cellule n'est jamais vide
     *         sans raison) ; {@code siVide} si aucun identifiant n'est fourni
     */
    public static String nomAffiche(Map<String, String> noms, String id, String siVide) {
        if (id == null || id.isBlank()) {
            return siVide;
        }
        return noms.getOrDefault(id, id);
    }
}
