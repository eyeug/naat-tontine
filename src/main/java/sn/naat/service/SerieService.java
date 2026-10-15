package sn.naat.service;

import sn.naat.dao.DemandeNouvelleSerieDAO;

import java.sql.SQLException;

/** Donne la série courante d'une tontine (1 + nombre de nouvelles séries validées). */
public class SerieService {

    private final DemandeNouvelleSerieDAO demandeNouvelleSerieDAO = new DemandeNouvelleSerieDAO();

    public int serieCourante(String tontineId) throws SQLException {
        return SerieRegles.serieCourante(demandeNouvelleSerieDAO.compterValidees(tontineId));
    }
}
