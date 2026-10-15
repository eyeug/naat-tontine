package sn.naat.service;

import java.math.BigDecimal;

/**
 * Règles des demandes de création de tontine (l'administrateur valide ou refuse).
 * <ul>
 *   <li><b>Qui peut demander :</b> un gérant (pour une tontine de plus) ou un compte qui n'est membre
 *       d'aucune tontine en cours (pour sa première, ou après la fin de sa tontine). Un membre ne le
 *       peut pas tant qu'il est adhérent d'une tontine non terminée.</li>
 *   <li><b>Combien :</b> une seule demande en attente par utilisateur ; tant que sa demande n'a pas
 *       été traitée, il ne peut pas en soumettre une autre.</li>
 * </ul>
 *
 * Fonction pure (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class DemandeCreationRegles {

    /** Nombre maximal de demandes de création en attente par utilisateur. */
    public static final int LIMITE_EN_ATTENTE = 1;

    private DemandeCreationRegles() { }

    public static final int LONGUEUR_MAX_NOM = 150;
    public static final int NB_MEMBRES_MIN = 2;

    /**
     * Les valeurs saisies dans le formulaire de demande sont-elles acceptables ?
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusFormulaire(String nom, BigDecimal montant, int nbMembres) {
        if (nom == null || nom.isBlank()) {
            return "Le nom de la tontine est obligatoire.";
        }
        if (nom.trim().length() > LONGUEUR_MAX_NOM) {
            return "Le nom de la tontine ne doit pas dépasser " + LONGUEUR_MAX_NOM + " caractères.";
        }
        if (montant == null || montant.signum() <= 0) {
            return "Le montant de la cotisation doit être supérieur à zéro.";
        }
        if (nbMembres < NB_MEMBRES_MIN) {
            return "Une tontine a besoin d'au moins " + NB_MEMBRES_MIN + " membres.";
        }
        return null;
    }

    /**
     * Ce compte a-t-il le droit de demander la création d'une tontine ?
     * Un gérant (Gérant est un Membre) le peut, ainsi qu'un compte qui n'est membre d'aucune tontine.
     * Un simple membre ne le peut pas : il le pourra quand toutes ses tontines seront terminées (il redevient alors un
     * simple utilisateur). Le rôle de gérant ne s'obtient qu'en faisant valider une demande de création de tontine.
     *
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusRole(boolean estMembre, boolean estGerant, boolean estAdministrateur) {
        if (estAdministrateur) {
            return "Un administrateur ne peut pas demander la création d'une tontine : il n'est ni gérant ni "
                    + "membre (rôles séparés). Utilisez un compte ordinaire.";
        }
        return refusRole(estMembre, estGerant);
    }

    public static String refusRole(boolean estMembre, boolean estGerant) {
        if (estMembre && !estGerant) {
            return "Seul un gérant, ou un compte qui n'est membre d'aucune tontine, peut demander la "
                    + "création d'une tontine. Vous pourrez le faire quand vos tontines seront terminées.";
        }
        return null;
    }

    /**
     * Une nouvelle demande peut-elle être soumise ?
     * @param nbDemandesEnAttente nombre de demandes de cet utilisateur pas encore traitées
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusSoumission(int nbDemandesEnAttente) {
        if (nbDemandesEnAttente >= LIMITE_EN_ATTENTE) {
            return "Vous avez déjà une demande de création de tontine en attente de validation par "
                    + "l'administrateur. Attendez sa décision avant d'en soumettre une autre.";
        }
        return null;
    }
}
