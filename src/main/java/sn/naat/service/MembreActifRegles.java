package sn.naat.service;

import sn.naat.model.Enums.StatutTontine;

import java.util.List;

/**
 * « Membre » n'est pas un statut permanent : un compte est membre tant qu'il est adhérent actif d'au
 * moins une tontine NON terminée. Quand toutes ses tontines sont clôturées (tous ses membres ont
 * reçu leur distribution), il redevient un simple utilisateur : il arrive sur l'accueil à la
 * connexion, peut de nouveau demander la création d'une tontine ou en rejoindre une autre.
 *
 * Fonction pure (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class MembreActifRegles {

    private MembreActifRegles() { }

    /**
     * @param statutsDeSesTontines statut de chacune des tontines où le compte a une adhésion active
     * @return vrai si au moins une de ces tontines n'est pas clôturée
     */
    public static boolean estMembreActif(List<StatutTontine> statutsDeSesTontines) {
        return statutsDeSesTontines.stream().anyMatch(s -> s != StatutTontine.CLOTUREE);
    }
}
