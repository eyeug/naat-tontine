package sn.naat.util;

import sn.naat.model.Enums.Frequence;

import java.time.LocalDate;

/**
 * Calcule la durée d'une série complète pour une TontineTourDeRole ou
 * TontineTirageAuSort : le nombre de cycles restants dépend du nombre de
 * membres actifs (une série complète = un cycle par membre), et l'espacement
 * dans le temps dépend de la fréquence de la tontine.
 * Ne s'applique pas à TontineFondsCommun, qui n'a pas de série fermée.
 */
public final class CalculDuree {

    private CalculDuree() { }

    /** Libellé lisible de la durée totale d'une série (ex. "5 semaines", "5 mois"). */
    public static String formaterDureeLabel(int nbMembres, Frequence frequence) {
        if (nbMembres <= 0) return "À définir (aucun membre pour l'instant)";
        String unite = frequence == Frequence.HEBDOMADAIRE ? "semaine" : "mois";
        String pluriel = (frequence == Frequence.HEBDOMADAIRE && nbMembres > 1) ? "s" : "";
        return nbMembres + " " + unite + pluriel;
    }

    /**
     * Date de fin prévue de la série en cours, à partir du cycle actuel.
     * cyclesRestants = nbMembresActifs - (numeroCycleActuel - 1), borné à 0.
     */
    public static LocalDate calculerDateFin(LocalDate dateDebutCycleActuel, int numeroCycleActuel,
                                             int nbMembresActifs, Frequence frequence) {
        int cyclesRestants = Math.max(nbMembresActifs - (numeroCycleActuel - 1), 0);
        return frequence == Frequence.HEBDOMADAIRE
                ? dateDebutCycleActuel.plusWeeks(cyclesRestants)
                : dateDebutCycleActuel.plusMonths(cyclesRestants);
    }
}
