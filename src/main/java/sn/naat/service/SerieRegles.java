package sn.naat.service;

import sn.naat.model.Cycle;
import sn.naat.model.Enums.StatutTontine;
import sn.naat.model.Tirage;

import java.util.List;

/**
 * Règles liées aux séries d'une tontine. Une série est une « manche » complète : tous les
 * membres reçoivent leur distribution une fois, puis la tontine est clôturée. Le gérant peut
 * alors demander une nouvelle série, que l'administrateur valide ou refuse.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class SerieRegles {

    private SerieRegles() { }

    /** Série courante = 1 + nombre de nouvelles séries déjà validées par l'administrateur. */
    public static int serieCourante(int nbNouvellesSeriesValidees) {
        return 1 + Math.max(0, nbNouvellesSeriesValidees);
    }

    /**
     * Ligne de résumé de la série en cours, affichée au gérant : elle explique pourquoi la
     * tontine n'est pas (encore) clôturée ou démarrée.
     */
    public static String resume(int serie, StatutTontine statut, int membresActifs, int membresDemandes,
                                int servis, int attendus) {
        String base = "Série n°" + serie + " — " + statut.getLibelle();
        return switch (statut) {
            case EN_PREPARATION -> base + " (" + membresActifs + "/" + membresDemandes + " membres"
                    + (membresActifs < membresDemandes ? " : le premier cycle démarrera une fois ce nombre atteint)" : ")");
            case EN_COURS, CLOTUREE -> base + " (" + servis + "/" + attendus + " membres servis)";
        };
    }

    /**
     * Les adhésions de l'ancienne série prennent fin quand la nouvelle est validée : tant qu'il reste des
     * prêts à rembourser (ou à trancher), on ne peut pas démarrer de nouvelle série, car les anciens
     * membres ne verraient plus la tontine pour les rembourser.
     *
     * @return null si aucun prêt n'est en suspens, sinon le message expliquant le refus
     */
    public static String refusPretsNonSoldes(int nbPretsNonSoldes) {
        if (nbPretsNonSoldes > 0) {
            return "Des prêts de cette tontine ne sont pas encore soldés (" + nbPretsNonSoldes
                    + " en attente ou à rembourser) : ils doivent être remboursés ou refusés avant de démarrer "
                    + "une nouvelle série, car les adhésions de la série précédente prennent fin.";
        }
        return null;
    }

    /**
     * Ordre du tour de rôle au début d'une nouvelle série : seul le gérant est conservé (s'il y figurait).
     * Les autres y seront ajoutés au fur et à mesure que leur nouvelle adhésion est validée.
     */
    public static List<String> ordreDuTourNouvelleSerie(List<String> ancienOrdre, String gerantId) {
        return ancienOrdre.stream().filter(gerantId::equals).toList();
    }

    /** Cycles appartenant à une série donnée. */
    public static List<Cycle> cyclesDeLaSerie(List<Cycle> cycles, int serie) {
        return cycles.stream().filter(c -> c.getSerie() == serie).toList();
    }

    /** Tirages appartenant à une série donnée (ceux des séries précédentes ne comptent plus). */
    public static List<Tirage> tiragesDeLaSerie(List<Tirage> tirages, int serie) {
        return tirages.stream().filter(t -> t.getSerie() == serie).toList();
    }

    /**
     * Une demande de nouvelle série est-elle autorisée ?
     * @return null si oui, sinon le message expliquant le refus
     */
    public static String refusDemande(StatutTontine statut, boolean demandeDejaEnAttente) {
        if (statut != StatutTontine.CLOTUREE) {
            return "La série en cours n'est pas terminée : une nouvelle série ne peut être demandée "
                    + "qu'une fois la tontine clôturée (tous les membres ont reçu leur distribution).";
        }
        if (demandeDejaEnAttente) {
            return "Une demande de nouvelle série est déjà en attente de validation par l'administrateur.";
        }
        return null;
    }
}
