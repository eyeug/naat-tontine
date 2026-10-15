package sn.naat.service;

import sn.naat.dao.TontineDAO;
import sn.naat.model.Enums.StatutTontine;
import sn.naat.model.Tontine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Détermine si un compte est actuellement MEMBRE. Ce n'est pas la simple présence d'une ligne dans la
 * table membre (elle n'est jamais supprimée) : il faut une adhésion active dans une tontine qui n'est
 * pas terminée. Voir MembreActifRegles.
 *
 * Gérant et Administrateur, eux, restent des rôles permanents (tables gerant et administrateur).
 */
public class RoleService {

    private final TontineDAO tontineDAO = new TontineDAO();
    private final StatutTontineService statutTontineService = new StatutTontineService();

    public boolean estMembreActif(String utilisateurId) throws SQLException {
        List<StatutTontine> statuts = new ArrayList<>();
        for (Tontine t : tontineDAO.listerParMembre(utilisateurId)) {
            StatutTontine statut = statutTontineService.statutDe(t);
            if (statut != StatutTontine.CLOTUREE) {
                return true; // une tontine non terminée suffit : inutile d'examiner les autres
            }
            statuts.add(statut);
        }
        return MembreActifRegles.estMembreActif(statuts);
    }
}
