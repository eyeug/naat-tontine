package sn.naat.util;

import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.TypeTontine;

import java.time.LocalDate;

/**
 * Calcule la durée d'une tontine.
 * - Tour de rôle / Tirage au sort : une série complète dure exactement
 *   autant de cycles qu'il y a de membres (chacun reçoit son tour une
 *   fois), chaque cycle durant une semaine ou un mois selon la fréquence.
 * - Fonds commun : durée fixe d'UN AN, pendant laquelle chaque membre
 *   peut emprunter (plafonné au triple de son solde) et doit rembourser
 *   avant d'emprunter à nouveau — ce n'est pas une caisse perpétuelle,
 *   elle a un terme comme les deux autres types.
 */
public final class DureeTontine {

    private DureeTontine() { }

    private static final int DUREE_FONDS_COMMUN_JOURS = 365;

    /** Nombre de jours d'un seul cycle, selon la fréquence. */
    public static int joursParCycle(Frequence frequence) {
        return frequence == Frequence.HEBDOMADAIRE ? 7 : 30;
    }

    /** Libellé lisible, ex. "5 semaines", "5 mois", ou "1 an" pour Fonds commun. */
    public static String genererDureeLabel(TypeTontine type, int nbMembres, Frequence frequence) {
        if (type == TypeTontine.FONDS_COMMUN) return "1 an";
        if (nbMembres <= 0) return "À définir";
        String unite = frequence == Frequence.HEBDOMADAIRE ? "semaine" : "mois";
        return nbMembres + " " + unite + (nbMembres > 1 && frequence == Frequence.HEBDOMADAIRE ? "s" : "");
    }

    /**
     * Date de fin prévue, à partir d'une date de départ :
     * - Fonds commun : dateDebut + 1 an, fixe, peu importe le nombre de membres.
     * - Tour de rôle / Tirage au sort : dateDebut + (nbMembres × durée d'un cycle).
     */
    public static LocalDate dateFinPrevue(LocalDate dateDebut, TypeTontine type, int nbMembres, Frequence frequence) {
        if (dateDebut == null) return null;
        if (type == TypeTontine.FONDS_COMMUN) return dateDebut.plusDays(DUREE_FONDS_COMMUN_JOURS);
        return dateDebut.plusDays((long) nbMembres * joursParCycle(frequence));
    }
}
