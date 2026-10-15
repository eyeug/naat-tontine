package sn.naat.service;

import sn.naat.model.Cycle;
import sn.naat.model.Enums.StatutCycle;
import sn.naat.model.Enums.StatutTontine;
import sn.naat.model.Enums.TypeTontine;
import sn.naat.model.Tontine;
import sn.naat.model.TontineFondsCommun;
import sn.naat.model.TontineTourDeRole;
import sn.naat.util.DureeTontine;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Règle de calcul du statut d'une tontine, affiché sur l'accueil.
 * Le statut n'est pas stocké : il se déduit des cycles et des versements
 * déjà effectués, donc il ne peut jamais être en désaccord avec eux.
 *
 * <ul>
 *   <li><b>En préparation</b> : aucun cycle n'a jamais été démarré.</li>
 *   <li><b>En cours</b> : un cycle est actif, ou la tontine est entre deux cycles
 *       et certains membres n'ont pas encore reçu leur distribution.</li>
 *   <li><b>Clôturée</b> : aucun cycle actif <b>et tous les membres ont reçu leur
 *       distribution</b> (une par membre sur la série) :
 *       <ul>
 *         <li>Tour de rôle : tous les membres de la rotation ont été servis ;</li>
 *         <li>Tirage au sort : tous les membres actifs ont été servis ;</li>
 *         <li>Fonds commun : tous les membres actifs ont été servis, et la durée
 *             d'un an est écoulée.</li>
 *       </ul></li>
 * </ul>
 *
 * On s'appuie sur les versements réellement effectués, pas sur le nombre de
 * cycles clôturés : si un même membre était payé deux fois (tour non avancé),
 * le nombre de cycles atteindrait celui des membres alors que certains
 * n'auraient encore rien reçu.
 *
 * Fonction pure (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class StatutTontineRegles {

    private StatutTontineRegles() { }

    /**
     * @param membresActifs ids des membres actuellement adhérents (actifs) de la tontine
     * @param membresServis ids des membres ayant reçu au moins un versement sur la tontine
     */
    public static StatutTontine calculer(Tontine tontine, List<Cycle> cycles,
                                         Set<String> membresActifs, Set<String> membresServis,
                                         LocalDate aujourdhui) {
        if (cycles.isEmpty()) {
            return StatutTontine.EN_PREPARATION;
        }
        boolean cycleEnCours = cycles.stream().anyMatch(c -> c.getStatut() == StatutCycle.EN_COURS);
        if (cycleEnCours) {
            return StatutTontine.EN_COURS;
        }
        // Aucun cycle actif : la tontine est soit terminée, soit simplement entre deux cycles.
        return serieTerminee(tontine, cycles, membresActifs, membresServis, aujourdhui)
                ? StatutTontine.CLOTUREE
                : StatutTontine.EN_COURS;
    }

    /**
     * Membres qui doivent avoir reçu leur distribution pour que la série soit terminée.
     * Pour le tour de rôle, ce sont les membres ACTIFS de la rotation réelle (ordre du tour) quand elle est
     * renseignée ; sinon, les membres actifs.
     */
    public static Set<String> membresAttendus(Tontine tontine, Set<String> membresActifs) {
        if (tontine instanceof TontineTourDeRole tourDeRole && !tourDeRole.getOrdreTour().isEmpty()) {
            // Un membre désactivé n'est pas attendu : il ne reçoit pas de tour et ne doit pas bloquer la clôture.
            // S'il est réactivé avant la fin, il redevient attendu et passe à son tour.
            Set<String> rotation = new HashSet<>(tourDeRole.getOrdreTour());
            rotation.retainAll(membresActifs);
            return rotation;
        }
        return membresActifs;
    }

    private static boolean serieTerminee(Tontine tontine, List<Cycle> cycles,
                                         Set<String> membresActifs, Set<String> membresServis,
                                         LocalDate aujourdhui) {
        // Pour le tour de rôle on s'appuie sur la rotation réelle (ordre du tour)
        // quand elle est renseignée ; sinon sur les membres actifs.
        Set<String> attendus = membresAttendus(tontine, membresActifs);

        // Garde-fou : sans aucun membre attendu, on ne déclare pas la tontine terminée.
        boolean tousServis = !attendus.isEmpty() && membresServis.containsAll(attendus);

        if (tontine instanceof TontineFondsCommun) {
            // En plus, la durée fixe d'un an (à compter du premier cycle) doit être écoulée.
            LocalDate debut = cycles.stream()
                    .map(Cycle::getDateDebut)
                    .min(Comparator.naturalOrder())
                    .orElse(aujourdhui);
            LocalDate fin = DureeTontine.dateFinPrevue(debut, TypeTontine.FONDS_COMMUN,
                    tontine.getNbMembresMax(), tontine.getFrequence());
            return tousServis && !aujourdhui.isBefore(fin);
        }
        return tousServis;
    }
}
