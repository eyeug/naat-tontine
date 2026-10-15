package sn.naat.service;

import java.math.BigDecimal;

/**
 * Où en est un membre vis-à-vis du cycle en cours : a-t-il cotisé, ou doit-il encore le faire avant la
 * clôture ? Un retard n'est enregistré qu'à la clôture du cycle ; cet indicateur le prévient AVANT.
 *
 * Le critère est exactement celui qui déclenche la pénalité à la clôture (aucune cotisation depuis le début
 * du cycle), donc l'indicateur annonce fidèlement ce qui va se passer.
 *
 * Fonction pure (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class EtatCotisationRegles {

    private EtatCotisationRegles() { }

    /**
     * @param message le texte à afficher au membre
     * @param aFaire  vrai si une cotisation est encore attendue (l'écran l'affiche alors en rouge)
     */
    public record EtatCotisation(String message, boolean aFaire) { }

    public static EtatCotisation calculer(boolean cycleEnCours, int numeroCycle, boolean adhesionActive,
                                          boolean dejaCotise, BigDecimal penaliteRetard) {
        if (!adhesionActive) {
            return new EtatCotisation("Votre adhésion à cette tontine est désactivée : aucune cotisation n'est "
                    + "attendue de votre part.", false);
        }
        if (!cycleEnCours) {
            return new EtatCotisation("Aucun cycle en cours : rien à cotiser pour le moment.", false);
        }
        if (dejaCotise) {
            return new EtatCotisation("Cycle n°" + numeroCycle + " en cours : ✓ vous avez déjà cotisé.", false);
        }
        String penalite = (penaliteRetard != null && penaliteRetard.signum() > 0)
                ? " Sans cotisation, une pénalité de " + penaliteRetard + " FCFA sera déduite de votre solde."
                : "";
        return new EtatCotisation("Cycle n°" + numeroCycle + " en cours : ⚠ cotisation à faire avant la clôture "
                + "du cycle." + penalite, true);
    }
}
