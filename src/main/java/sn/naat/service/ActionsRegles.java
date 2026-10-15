package sn.naat.service;

import sn.naat.model.Tontine;
import sn.naat.model.TontineFondsCommun;
import sn.naat.model.TontineTourDeRole;

/**
 * Quelles actions de l'espace Membre s'appliquent à une tontine, selon son TYPE. Ce qui ne s'applique jamais
 * est masqué (inutile de proposer un bouton qui répond toujours « impossible ») ; ce qui s'applique mais n'est
 * pas possible pour l'instant est grisé avec sa raison (voir MembreService.verifierPeutDemanderTourAnticipe).
 *
 * Fonction pure (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class ActionsRegles {

    private ActionsRegles() { }

    /**
     * @param demanderPret    « Demander un prêt » : réservé au Fonds commun
     * @param rembourserPret  « Rembourser un prêt » : réservé au Fonds commun
     * @param tourAnticipe    « Demander un tour anticipé » : réservé au Tour de rôle
     */
    public record Actions(boolean demanderPret, boolean rembourserPret, boolean tourAnticipe) { }

    /** Actions proposées pour la tontine sélectionnée ; aucune si rien n'est sélectionné. */
    public static Actions pour(Tontine tontine) {
        boolean fondsCommun = tontine instanceof TontineFondsCommun;
        boolean tourDeRole = tontine instanceof TontineTourDeRole;
        return new Actions(fondsCommun, fondsCommun, tourDeRole);
    }
}
