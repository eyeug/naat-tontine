package sn.naat.service;

import sn.naat.model.Enums.StatutTontine;

import java.util.ArrayList;
import java.util.List;

/**
 * Règles du tour anticipé : dans une tontine Tour de rôle, un membre demande à recevoir sa distribution plus
 * tôt que prévu. Si le gérant accorde, le membre est placé JUSTE APRÈS le bénéficiaire actuel : il sera donc
 * le prochain à recevoir, quand le cycle en cours sera clôturé. Le bénéficiaire actuel, lui, n'est pas déplacé :
 * le cycle en cours garde son bénéficiaire.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class TourAnticipeRegles {

    private TourAnticipeRegles() { }

    /**
     * Une demande de tour anticipé est-elle possible ? Contrôles dans cet ordre, le premier refus l'emporte.
     * Le gérant refait les mêmes contrôles quand il accorde (la situation a pu changer depuis la demande),
     * en passant false pour {@code demandeEnAttente} et {@code estProchainBeneficiaire}.
     *
     * @return null si la demande est possible, sinon le message expliquant le refus
     */
    public static String refusDemande(boolean tourDeRole, StatutTontine statut, boolean adhesionActive,
                                      boolean dansLaRotation, boolean dejaServi, boolean estBeneficiaireActuel,
                                      boolean estProchainBeneficiaire, boolean demandeEnAttente) {
        if (!tourDeRole) {
            return "Le tour anticipé n'existe que pour les tontines de type Tour de rôle.";
        }
        if (statut == StatutTontine.CLOTUREE) {
            return "Cette tontine est terminée : il n'y a plus de tour à avancer.";
        }
        if (!adhesionActive) {
            return CotisationRegles.refusAdhesionInactive(false, true);
        }
        if (!dansLaRotation) {
            return "Vous ne figurez pas dans l'ordre du tour de cette tontine.";
        }
        if (dejaServi) {
            return "Vous avez déjà reçu votre distribution dans cette série : un tour anticipé n'a plus de sens.";
        }
        if (estBeneficiaireActuel) {
            return "C'est déjà votre tour : vous êtes le bénéficiaire actuel.";
        }
        if (estProchainBeneficiaire) {
            return "Vous êtes déjà le prochain bénéficiaire : un tour anticipé ne vous ferait rien gagner.";
        }
        if (demandeEnAttente) {
            return "Vous avez déjà une demande de tour anticipé en attente sur cette tontine.";
        }
        return null;
    }

    /** Nouvel ordre du tour et nouvelle position du bénéficiaire actuel, après un tour anticipé accordé. */
    public record Rotation(List<String> ordre, int indexCourant) { }

    /**
     * Place le membre juste après le bénéficiaire actuel. Si le membre était situé avant le bénéficiaire
     * actuel (par exemple sauté parce qu'il avait été désactivé), le retirer décale le bénéficiaire actuel
     * d'un cran vers le début : on le recalcule pour que le bénéficiaire actuel reste le même.
     */
    public static Rotation apresTourAnticipe(List<String> ordre, int indexCourant, String membreId) {
        int position = ordre.indexOf(membreId);
        if (position < 0) {
            throw new IllegalArgumentException("Le membre ne figure pas dans l'ordre du tour.");
        }
        List<String> nouvelOrdre = new ArrayList<>(ordre);
        nouvelOrdre.remove(position);
        int courant = position < indexCourant ? indexCourant - 1 : indexCourant;
        nouvelOrdre.add(courant + 1, membreId);
        return new Rotation(nouvelOrdre, courant);
    }
}
