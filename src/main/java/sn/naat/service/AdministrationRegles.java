package sn.naat.service;

/**
 * Règles de protection des comptes administrateurs.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class AdministrationRegles {

    /**
     * Peut-on retirer le rôle d'administrateur à ce compte ? Le compte garde ses autres rôles (membre, gérant)
     * et son accès : seul le rôle d'administrateur est retiré, ce qui libère une place sous la limite.
     * Refusé dans trois cas, vérifiés dans cet ordre :
     * <ol>
     *   <li>le compte n'est pas administrateur ;</li>
     *   <li>c'est le dernier administrateur actif (plus personne ne pourrait administrer l'application) ;</li>
     *   <li>c'est le compte de celui qui agit : retirer son propre rôle est presque toujours une erreur de clic,
     *       et laisserait une session ouverte avec des droits qu'elle n'a plus.</li>
     * </ol>
     *
     * @return null si le retrait est possible, sinon le message expliquant le refus
     */
    public static String refusRetraitRole(boolean cibleEstAdministrateur, boolean cibleEstActive,
                                          int nbAdministrateursActifs, boolean cibleEstSoiMeme) {
        if (!cibleEstAdministrateur) {
            return "Ce compte n'est pas administrateur.";
        }
        if (cibleEstActive && nbAdministrateursActifs <= 1) {
            return "Impossible de retirer le rôle du dernier administrateur actif : plus personne ne pourrait "
                    + "administrer l'application. Promouvez d'abord un autre administrateur.";
        }
        if (cibleEstSoiMeme) {
            return "Vous ne pouvez pas retirer votre propre rôle d'administrateur : demandez-le à l'autre "
                    + "administrateur.";
        }
        return null;
    }

    /**
     * Séparation des rôles : un administrateur n'est NI gérant NI membre, et réciproquement. Sinon il pourrait
     * valider lui-même ses propres demandes (créer une tontine, ouvrir une nouvelle série, obtenir un prêt) :
     * un conflit d'intérêts. Pour participer à une tontine, il utilise un autre compte, ordinaire.
     *
     * @return null si la promotion en gérant est possible, sinon le message expliquant le refus
     */
    public static String refusPromotionGerant(boolean estAdministrateur) {
        if (estAdministrateur) {
            return "Un administrateur ne peut pas être gérant : les rôles sont séparés pour qu'il ne valide jamais "
                    + "ses propres demandes. Créez un compte ordinaire pour ce rôle.";
        }
        return null;
    }

    /**
     * Un administrateur ne peut pas non plus devenir membre d'une tontine (demande d'adhésion).
     *
     * @return null si l'adhésion est possible, sinon le message expliquant le refus
     */
    public static String refusAdhesionAdministrateur(boolean estAdministrateur) {
        if (estAdministrateur) {
            return "Un administrateur ne peut pas rejoindre une tontine : les rôles sont séparés. "
                    + "Utilisez un compte ordinaire.";
        }
        return null;
    }

    /**
     * Même règle que {@link #refusPromotion(boolean, int)}, avec en plus la séparation des rôles : on ne promeut
     * pas en administrateur un compte qui est gérant ou membre actif. Ordre des contrôles : déjà administrateur,
     * cumul de rôles, puis limite du nombre d'administrateurs.
     *
     * @param estGerantOuMembreActif le compte est gérant, ou membre actif d'une tontine non clôturée
     */
    public static String refusPromotion(boolean dejaAdministrateur, boolean estGerantOuMembreActif, int nbAdministrateurs) {
        if (dejaAdministrateur) {
            return "Ce compte est déjà administrateur.";
        }
        if (estGerantOuMembreActif) {
            return "Ce compte est gérant ou membre d'une tontine : un administrateur ne peut être ni gérant ni "
                    + "membre (rôles séparés). Utilisez un compte distinct pour l'administration.";
        }
        return refusPromotion(false, nbAdministrateurs);
    }

    /** Nombre maximal de comptes administrateurs, par sécurité : chacun a tous les pouvoirs. */
    public static final int LIMITE_ADMINISTRATEURS = 2;

    private AdministrationRegles() { }

    /**
     * Peut-on promouvoir ce compte en administrateur ? Pas au-delà de {@link #LIMITE_ADMINISTRATEURS}
     * comptes administrateurs : chacun a tous les pouvoirs, donc plus il y en a, plus un seul mot de passe
     * volé met toute l'application en danger. On compte TOUS les comptes ayant le rôle, suspendus compris :
     * un administrateur suspendu peut être réactivé, il garde donc sa place.
     *
     * @param dejaAdministrateur   le compte visé est déjà administrateur
     * @param nbAdministrateurs    nombre de comptes administrateurs (suspendus compris)
     * @return null si la promotion est possible, sinon le message expliquant le refus
     */
    public static String refusPromotion(boolean dejaAdministrateur, int nbAdministrateurs) {
        if (dejaAdministrateur) {
            return "Ce compte est déjà administrateur.";
        }
        if (nbAdministrateurs >= LIMITE_ADMINISTRATEURS) {
            return "Le nombre d'administrateurs est limité à " + LIMITE_ADMINISTRATEURS + " pour des raisons de "
                    + "sécurité : la limite est atteinte, on ne peut plus promouvoir d'autre administrateur.";
        }
        return null;
    }

    /**
     * Peut-on suspendre ce compte ? Jamais le dernier administrateur actif : l'application n'aurait plus
     * personne pour valider les créations de tontines, les nouvelles séries ou les prêts des gérants, ni pour
     * réactiver un compte, et la seule issue serait de modifier la base de données à la main.
     * Cela protège aussi d'un clic involontaire sur son propre compte.
     *
     * @param cibleEstAdministrateurActif le compte visé est un administrateur dont le compte est actif
     * @param nbAdministrateursActifs     nombre d'administrateurs dont le compte est actif (le visé compris)
     * @return null si la suspension est possible, sinon le message expliquant le refus
     */
    public static String refusSuspension(boolean cibleEstAdministrateurActif, int nbAdministrateursActifs) {
        if (cibleEstAdministrateurActif && nbAdministrateursActifs <= 1) {
            return "Impossible de suspendre le dernier administrateur actif : plus personne ne pourrait "
                    + "administrer l'application. Promouvez d'abord un autre administrateur.";
        }
        return null;
    }
}
