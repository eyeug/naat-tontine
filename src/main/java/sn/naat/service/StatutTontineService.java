package sn.naat.service;

import sn.naat.dao.AdhesionDAO;
import sn.naat.dao.CycleDAO;
import sn.naat.dao.TransactionDAO;
import sn.naat.model.Adhesion;
import sn.naat.model.Cycle;
import sn.naat.model.Enums.StatutAdhesion;
import sn.naat.model.Enums.StatutTontine;
import sn.naat.model.Tontine;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Charge les données nécessaires et applique StatutTontineRegles. Partagé par
 * l'accueil (affichage du statut) et par le gérant (refuser de démarrer un cycle
 * sur une tontine déjà clôturée), pour que les deux s'accordent toujours.
 */
public class StatutTontineService {

    private final CycleDAO cycleDAO = new CycleDAO();
    private final AdhesionDAO adhesionDAO = new AdhesionDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final SerieService serieService = new SerieService();

    /** Données de la SÉRIE COURANTE : les cycles et versements des séries précédentes ne comptent plus. */
    private record Donnees(int serie, List<Cycle> cycles, Set<String> membresActifs, Set<String> membresServis) { }

    private Donnees charger(Tontine tontine) throws SQLException {
        int serie = serieService.serieCourante(tontine.getId());
        List<Cycle> cycles = SerieRegles.cyclesDeLaSerie(cycleDAO.listerParTontine(tontine.getId()), serie);
        Set<String> membresActifs = adhesionDAO.listerParTontine(tontine.getId()).stream()
                .filter(a -> a.getStatut() == StatutAdhesion.ACTIF)
                .map(Adhesion::getMembreId)
                .collect(Collectors.toSet());
        Set<String> membresServis = transactionDAO.membresAyantRecuVersement(tontine.getId(), serie);
        return new Donnees(serie, cycles, membresActifs, membresServis);
    }

    /**
     * Statut de la tontine pour sa série courante. Quand l'administrateur valide une nouvelle
     * série, la tontine repasse « En préparation » et chaque membre redevient « pas encore servi ».
     */
    public StatutTontine statutDe(Tontine tontine) throws SQLException {
        Donnees d = charger(tontine);
        return StatutTontineRegles.calculer(tontine, d.cycles(), d.membresActifs(), d.membresServis(), LocalDate.now());
    }

    /** Statut de plusieurs tontines : table id de tontine → statut. */
    public Map<String, StatutTontine> statutsDe(List<Tontine> tontines) throws SQLException {
        Map<String, StatutTontine> statuts = new HashMap<>();
        for (Tontine t : tontines) {
            statuts.put(t.getId(), statutDe(t));
        }
        return statuts;
    }

    /**
     * Résumé de la série en cours, ex. « Série n°1 — En cours (3/5 membres servis) » ou
     * « Série n°1 — En préparation (3/5 membres : le premier cycle démarrera une fois ce nombre atteint) ».
     */
    public String resume(Tontine tontine) throws SQLException {
        Donnees d = charger(tontine);
        StatutTontine statut = StatutTontineRegles.calculer(tontine, d.cycles(), d.membresActifs(),
                d.membresServis(), LocalDate.now());
        Set<String> attendus = StatutTontineRegles.membresAttendus(tontine, d.membresActifs());
        int servis = (int) attendus.stream().filter(d.membresServis()::contains).count();
        return SerieRegles.resume(d.serie(), statut, d.membresActifs().size(), tontine.getNbMembresMax(),
                servis, attendus.size());
    }
}
