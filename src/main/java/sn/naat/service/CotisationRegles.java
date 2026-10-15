package sn.naat.service;

import sn.naat.dao.CycleDAO;
import sn.naat.dao.TransactionDAO;
import sn.naat.model.Cycle;
import sn.naat.model.Tontine;
import sn.naat.model.TontineTirageAuSort;
import sn.naat.model.TontineTourDeRole;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Règles communes à toute cotisation, qu'elle soit faite par le membre en
 * ligne (MembreService.cotiser) ou saisie par le gérant en espèces
 * (GerantService.saisirCotisationEspeces). Les centraliser ici évite que les
 * deux chemins appliquent des règles différentes.
 *
 * <ul>
 *   <li>Tour de rôle et Tirage au sort : montant fixe, identique pour tous
 *       (c'est le gérant qui le définit à la création de la tontine).</li>
 *   <li>Fonds commun : montant libre, mais compris dans la fourchette [min ; max] fixée par le gérant
 *       dans son projet et validée par l'administrateur (voir MontantCotisationRegles).</li>
 *   <li>Un cycle actif est nécessaire, et une seule cotisation est permise
 *       par membre et par cycle.</li>
 * </ul>
 */
public final class CotisationRegles {

    private static final CycleDAO cycleDAO = new CycleDAO();
    private static final TransactionDAO transactionDAO = new TransactionDAO();

    private CotisationRegles() { }

    /**
     * Un membre dont l'adhésion a été désactivée par son gérant ne peut plus cotiser, demander de prêt
     * ni de tour anticipé. Il peut en revanche toujours REMBOURSER un prêt en cours : on ne peut pas lui
     * interdire de payer ce qu'il doit (rembourserPret ne passe donc pas par cette règle).
     *
     * @param adhesionActive vrai si l'adhésion est active
     * @param pourLeMembre   vrai si le message s'adresse au membre lui-même (« Votre adhésion… »), faux s'il
     *                       s'adresse au gérant qui agit à sa place (cotisation en espèces)
     * @return null si l'adhésion est active, sinon le message expliquant le refus
     */
    public static String refusAdhesionInactive(boolean adhesionActive, boolean pourLeMembre) {
        if (adhesionActive) {
            return null;
        }
        return pourLeMembre
                ? "Votre adhésion à cette tontine a été désactivée par le gérant : vous ne pouvez plus cotiser, "
                  + "demander de prêt ni de tour anticipé. Vous pouvez toujours rembourser un prêt en cours. "
                  + "Contactez le gérant."
                : "L'adhésion de ce membre à cette tontine est désactivée : aucune cotisation ne peut être "
                  + "enregistrée pour lui.";
    }

    /**
     * Le montant de la cotisation est-il imposé par la tontine ?
     * Vrai pour Tour de rôle et Tirage au sort ; l'interface s'en sert pour
     * afficher le montant en lecture seule au lieu d'un champ de saisie.
     */
    public static boolean montantImpose(Tontine tontine) {
        return tontine instanceof TontineTourDeRole || tontine instanceof TontineTirageAuSort;
    }

    /**
     * Vérifie qu'une cotisation est autorisée, avant toute écriture.
     *
     * @throws IllegalArgumentException si le montant est invalide ou ne correspond pas au montant imposé
     * @throws IllegalStateException    si aucun cycle n'est actif, ou si le membre a déjà cotisé pour ce cycle
     */
    public static void verifier(Tontine tontine, String membreId, BigDecimal montant) throws SQLException {
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant de la cotisation doit être supérieur à zéro.");
        }
        if (montantImpose(tontine) && montant.compareTo(tontine.getMontantCotisation()) != 0) {
            throw new IllegalArgumentException("Le montant de la cotisation est fixé à "
                    + tontine.getMontantCotisation() + " FCFA pour cette tontine (identique pour tous les membres).");
        }

        String refusFourchette = MontantCotisationRegles.refusMontant(
                tontine.getMontantMin(), tontine.getMontantMax(), montant);
        if (refusFourchette != null) {
            throw new IllegalArgumentException(refusFourchette);
        }

        Cycle cycleActif = cycleDAO.trouverCycleEnCours(tontine.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun cycle actif sur cette tontine : le gérant doit d'abord démarrer un cycle."));
        if (transactionDAO.aDejaCotiseDepuis(tontine.getId(), membreId, cycleActif.getDateDebut().atStartOfDay())) {
            throw new IllegalStateException("Une cotisation a déjà été enregistrée pour le cycle n°"
                    + cycleActif.getNumero() + " (débuté le " + cycleActif.getDateDebut() + "). "
                    + "La prochaine sera possible au cycle suivant.");
        }
    }
}
