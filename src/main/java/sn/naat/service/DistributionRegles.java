package sn.naat.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Règles qui garantissent qu'un membre n'est payé qu'UNE SEULE FOIS par série
 * (une tontine = une série : chaque membre reçoit sa distribution une fois,
 * puis la tontine est clôturée).
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class DistributionRegles {

    private DistributionRegles() { }

    /**
     * Membres pouvant encore être tirés au sort : membres actifs qui n'ont
     * jamais été tirés ni déjà servis. Quand la liste est vide, la série est
     * terminée : on ne remet PAS tout le monde dans le tirage, sinon un même
     * membre pourrait gagner (et être payé) deux fois.
     *
     * @param membresActifs ids des membres actifs, dans l'ordre
     * @param dejaTires     ids des gagnants des tirages déjà effectués
     * @param servis        ids des membres ayant déjà reçu un versement
     */
    public static List<String> eligiblesAuTirage(List<String> membresActifs,
                                                 Collection<String> dejaTires,
                                                 Set<String> servis) {
        return membresActifs.stream()
                .filter(id -> !dejaTires.contains(id))
                .filter(id -> !servis.contains(id))
                .toList();
    }

    /** Ce membre a-t-il déjà reçu sa distribution sur cette série ? (faux si aucun bénéficiaire) */
    public static boolean dejaServi(String membreId, Set<String> servis) {
        return membreId != null && servis.contains(membreId);
    }

    /**
     * Membres de la rotation à ÉCARTER du tour : ceux déjà servis, plus ceux dont l'adhésion est désactivée.
     * Passé à prochainNonServi() et rotationTerminee(), cela fait sauter les membres désactivés (ils ne sont
     * jamais désignés comme bénéficiaires) et ne les compte pas pour décider que la rotation est terminée.
     */
    public static Set<String> servisOuDesactives(List<String> ordreDuTour, Set<String> servis, Set<String> membresActifs) {
        Set<String> ecartes = new java.util.HashSet<>(servis);
        for (String membreId : ordreDuTour) {
            if (!membresActifs.contains(membreId)) {
                ecartes.add(membreId);
            }
        }
        return ecartes;
    }

    /**
     * Le bénéficiaire désigné pour ce cycle peut-il recevoir la distribution ? Pas s'il a été désactivé par le gérant.
     *
     * @return null s'il est actif, sinon le message expliquant le refus (avec la marche à suivre selon le type)
     */
    public static String refusBeneficiaireDesactive(boolean beneficiaireActif, boolean tourDeRole, String nom) {
        if (beneficiaireActif) {
            return null;
        }
        return nom + " a été désactivé : il ne peut pas recevoir la distribution. "
                + (tourDeRole
                    ? "Avancez le tour pour désigner le prochain bénéficiaire, puis clôturez le cycle."
                    : "Réactivez son adhésion pour pouvoir clôturer ce cycle.");
    }

    /**
     * Ligne affichée dans l'ordre du tour (vue des membres). Se fonde sur ce qui s'est réellement passé
     * (qui a été servi, qui est désactivé), et non sur la position : avec des tours sautés, un membre placé
     * avant le tour courant n'a pas forcément reçu sa distribution.
     */
    public static String libelleOrdreDuTour(int position, String nom, boolean servi, boolean beneficiaireActuel, boolean actif) {
        String statut = servi ? "✓ a déjà reçu"
                : !actif ? "désactivé : tour sauté"
                : beneficiaireActuel ? "→ bénéficiaire actuel"
                : "en attente";
        return position + ". " + nom + " — " + statut;
    }

    /**
     * Index du prochain membre de la rotation qui n'a pas encore reçu sa distribution :
     * on part de celui qui suit l'index courant et on repart du début au besoin, en sautant
     * les membres déjà servis. Ainsi « Avancer le tour » ne désigne jamais quelqu'un qui a
     * déjà été payé. Retourne -1 si la rotation est vide ou si tous ont déjà été servis.
     */
    public static int prochainNonServi(List<String> ordreDuTour, int indexCourant, Set<String> servis) {
        int n = ordreDuTour.size();
        if (n == 0) return -1;
        int depart = Math.floorMod(indexCourant, n);
        for (int pas = 1; pas <= n; pas++) {
            int candidat = (depart + pas) % n;
            if (!servis.contains(ordreDuTour.get(candidat))) return candidat;
        }
        return -1;
    }

    /** Tous les membres de la rotation ont-ils déjà reçu leur distribution ? (faux si la rotation est vide) */
    public static boolean rotationTerminee(List<String> ordreDuTour, Set<String> servis) {
        return !ordreDuTour.isEmpty() && servis.containsAll(ordreDuTour);
    }
}
