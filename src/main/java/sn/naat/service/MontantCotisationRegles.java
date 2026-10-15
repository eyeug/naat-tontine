package sn.naat.service;

import java.math.BigDecimal;

/**
 * Fourchette de cotisation d'une tontine « Fonds commun » (montant libre, mais encadré).
 *
 * <ul>
 *   <li><b>Qui la fixe :</b> le gérant, dans son projet de tontine (demande de création).</li>
 *   <li><b>Qui la contrôle :</b> l'administrateur, qui valide ou refuse la demande ; le système
 *       applique en plus des garde-fous (minimum absolu ; aucun plafond sur le maximum).</li>
 *   <li><b>Pour qui :</b> chaque membre cotise le montant de son choix, à l'intérieur de la fourchette.
 *       Une fois la tontine créée, la fourchette ne change plus (équité envers les membres).</li>
 * </ul>
 *
 * Fonction pure (aucun accès à la base).
 */
public final class MontantCotisationRegles {

    /** Plus petit minimum admis pour une cotisation (FCFA). */
    public static final BigDecimal MINIMUM_ABSOLU = new BigDecimal("100");

    private MontantCotisationRegles() { }

    /** @return null si la fourchette est acceptable, sinon le message expliquant le refus */
    public static String refusFourchette(BigDecimal min, BigDecimal max) {
        if (min == null || max == null || min.signum() <= 0 || max.signum() <= 0) {
            return "Indiquez un montant minimum et un montant maximum de cotisation, supérieurs à zéro.";
        }
        if (min.compareTo(MINIMUM_ABSOLU) < 0) {
            return "Le montant minimum de cotisation ne peut pas être inférieur à " + MINIMUM_ABSOLU.toPlainString() + " FCFA.";
        }
        if (max.compareTo(min) < 0) {
            return "Le montant maximum doit être supérieur ou égal au montant minimum.";
        }
        return null;
    }

    /**
     * Le montant versé respecte-t-il la fourchette ? Sans fourchette définie (anciennes tontines), tout montant
     * strictement positif est accepté.
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusMontant(BigDecimal min, BigDecimal max, BigDecimal montant) {
        if (min == null || max == null) {
            return null;
        }
        if (montant.compareTo(min) < 0 || montant.compareTo(max) > 0) {
            return "La cotisation doit être comprise entre " + min.toPlainString() + " et "
                    + max.toPlainString() + " FCFA pour cette tontine.";
        }
        return null;
    }

    /** Texte d'affichage : « 1000 – 5000 FCFA », ou « 2000 FCFA » si min = max, ou le montant seul sans fourchette. */
    public static String libelle(BigDecimal min, BigDecimal max, BigDecimal montantParDefaut) {
        if (min == null || max == null) {
            return montantParDefaut.toPlainString() + " FCFA";
        }
        if (min.compareTo(max) == 0) {
            return min.toPlainString() + " FCFA";
        }
        return min.toPlainString() + " – " + max.toPlainString() + " FCFA";
    }
}
