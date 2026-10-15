package sn.naat.service;

import sn.naat.model.Enums.StatutTontine;

/**
 * Règles liées au nombre de membres demandé pour une tontine (celui saisi à la création).
 * Tant que ce nombre n'est pas atteint, la tontine reste « En préparation » : son premier
 * cycle ne peut pas démarrer. Une fois atteint, elle est complète : plus aucune adhésion.
 * Ainsi le nombre de membres reste fixe pendant toute la série, et la durée de la série
 * (un cycle par membre) est bien celle annoncée.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class CapaciteRegles {

    private CapaciteRegles() { }

    /**
     * Le premier cycle d'une série peut-il démarrer ?
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusPremierCycle(int nbMembresActifs, int nbMembresDemandes) {
        if (nbMembresActifs < nbMembresDemandes) {
            return "La tontine n'est pas encore complète : " + nbMembresActifs + " membre(s) sur "
                    + nbMembresDemandes + " demandés. Le premier cycle ne peut démarrer qu'une fois "
                    + "ce nombre atteint ; la tontine reste « En préparation » d'ici là.";
        }
        return null;
    }

    /**
     * Peut-on réactiver un membre désactivé ? Pas si la série est terminée (« terminée » reste terminée :
     * rouvrir la série en y ramenant un membre qui n'a pas été servi n'a pas de sens), ni si la tontine est
     * déjà complète (un autre membre a pu rejoindre à sa place : le nombre demandé serait dépassé).
     *
     * @return null si la réactivation est possible, sinon le message expliquant le refus
     */
    public static String refusReactivation(StatutTontine statut, int nbMembresActifs, int nbMembresDemandes) {
        if (statut == StatutTontine.CLOTUREE) {
            return "Cette tontine est terminée : on ne peut plus réactiver de membre.";
        }
        if (nbMembresActifs >= nbMembresDemandes) {
            return "La tontine est complète (" + nbMembresActifs + "/" + nbMembresDemandes + " membres) : "
                    + "réactiver ce membre dépasserait le nombre de membres demandé.";
        }
        return null;
    }

    /**
     * Peut-on demander à rejoindre une tontine ? Pas si elle est terminée : ses membres ne sont plus membres
     * (leur adhésion prend fin quand l'administrateur valide la série suivante) et elle est complète. Le
     * message l'explique, au lieu d'un trompeur « vous êtes déjà membre ».
     *
     * @return null si la tontine n'est pas terminée, sinon le message expliquant le refus
     */
    public static String refusTontineTerminee(StatutTontine statut) {
        if (statut == StatutTontine.CLOTUREE) {
            return "Cette tontine est terminée : on ne peut pas la rejoindre pour le moment. Si son gérant demande "
                    + "une nouvelle série et que l'administrateur la valide, vous pourrez alors demander à la rejoindre.";
        }
        return null;
    }

    /**
     * Un compte peut-il demander à rejoindre une tontine où il a déjà une adhésion ?
     * La base n'autorise qu'une adhésion par couple (tontine, membre) : sans ce contrôle, la validation
     * échouerait sur un doublon. Une adhésion encore présente est soit active (série en cours), soit
     * désactivée par le gérant ; les adhésions d'une série terminée, elles, sont supprimées quand la série
     * suivante est validée, ce qui permet à un ancien membre de redemander à rejoindre.
     *
     * @param adhesionExiste  le compte a déjà une adhésion dans cette tontine
     * @param adhesionActive  cette adhésion est active (sinon, elle a été désactivée par le gérant)
     * @return null s'il n'y a pas d'adhésion, sinon le message expliquant le refus
     */
    public static String refusAdhesionExistante(boolean adhesionExiste, boolean adhesionActive) {
        if (!adhesionExiste) {
            return null;
        }
        return adhesionActive
                ? "Vous êtes déjà membre de cette tontine."
                : "Votre adhésion à cette tontine a été désactivée : contactez son gérant.";
    }

    /**
     * Une nouvelle adhésion peut-elle être acceptée ?
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusAdhesion(int nbMembresActifs, int nbMembresDemandes) {
        if (nbMembresActifs >= nbMembresDemandes) {
            return "La tontine est complète (" + nbMembresActifs + "/" + nbMembresDemandes
                    + " membres) : aucune nouvelle adhésion n'est possible.";
        }
        return null;
    }
}
