package sn.naat.dao;

import sn.naat.model.*;
import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.TypeTontine;
import sn.naat.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TontineDAO {

    public Tontine creer(Tontine t) throws SQLException {
        String sql = """
            INSERT INTO tontine (id, nom, description, type, frequence, duree_label, montant_cotisation,
                penalite_retard, nb_membres_max, gerant_id, taux_interet, duree_max_pret,
                ordre_tour, tour_actuel_index, montant_min, montant_max)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getNom());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getTypeCode().toLowerCase());
            ps.setString(5, t.getFrequence().name().toLowerCase());
            ps.setString(6, t.getDureeLabel());
            ps.setBigDecimal(7, t.getMontantCotisation());
            ps.setBigDecimal(8, t.getPenaliteRetard());
            ps.setInt(9, t.getNbMembresMax());
            ps.setString(10, t.getGerantId());

            if (t instanceof TontineFondsCommun fc) {
                ps.setBigDecimal(11, fc.getTauxInteret());
                ps.setInt(12, fc.getDureeMaxPret());
            } else {
                ps.setNull(11, Types.DECIMAL);
                ps.setNull(12, Types.INTEGER);
            }
            if (t instanceof TontineTourDeRole tr) {
                ps.setString(13, String.join(",", tr.getOrdreTour()));
                ps.setInt(14, tr.getTourActuelIndex());
            } else {
                ps.setNull(13, Types.VARCHAR);
                ps.setNull(14, Types.INTEGER);
            }
            ps.setBigDecimal(15, t.getMontantMin());
            ps.setBigDecimal(16, t.getMontantMax());
            ps.executeUpdate();
        }
        return t;
    }

    public List<Tontine> listerToutes() throws SQLException {
        List<Tontine> resultats = new ArrayList<>();
        String sql = "SELECT * FROM tontine ORDER BY date_creation DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    public List<Tontine> listerParGerant(String gerantId) throws SQLException {
        List<Tontine> resultats = new ArrayList<>();
        String sql = "SELECT * FROM tontine WHERE gerant_id = ? ORDER BY date_creation DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, gerantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    /** Tontines auxquelles un membre a une adhésion active. */
    public List<Tontine> listerParMembre(String membreId) throws SQLException {
        List<Tontine> resultats = new ArrayList<>();
        String sql = """
            SELECT t.* FROM tontine t
            JOIN adhesion a ON a.tontine_id = t.id
            WHERE a.membre_id = ? AND a.statut = 'actif'
            ORDER BY t.date_creation DESC
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, membreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    public Optional<Tontine> trouverParId(String id) throws SQLException {
        String sql = "SELECT * FROM tontine WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper(rs));
            }
        }
        return Optional.empty();
    }

    /** +definirParametres(...) côté Gerant — met à jour les attributs communs. */
    public void definirParametres(String tontineId, BigDecimal montantCotisation,
                                   BigDecimal penaliteRetard, int nbMembresMax) throws SQLException {
        String sql = """
            UPDATE tontine SET montant_cotisation = ?, penalite_retard = ?, nb_membres_max = ?
            WHERE id = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, montantCotisation);
            ps.setBigDecimal(2, penaliteRetard);
            ps.setInt(3, nbMembresMax);
            ps.setString(4, tontineId);
            ps.executeUpdate();
        }
    }

    public void mettreAJourTourDeRole(String tontineId, List<String> ordreTour, int tourActuelIndex) throws SQLException {
        String sql = "UPDATE tontine SET ordre_tour = ?, tour_actuel_index = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, String.join(",", ordreTour));
            ps.setInt(2, tourActuelIndex);
            ps.setString(3, tontineId);
            ps.executeUpdate();
        }
    }

    private Tontine mapper(ResultSet rs) throws SQLException {
        TypeTontine type = TypeTontine.valueOf(rs.getString("type").toUpperCase());
        Frequence freq = Frequence.valueOf(rs.getString("frequence").toUpperCase());

        Tontine t = switch (type) {
            case FONDS_COMMUN -> {
                TontineFondsCommun fc = new TontineFondsCommun();
                fc.setTauxInteret(rs.getBigDecimal("taux_interet"));
                fc.setDureeMaxPret(rs.getInt("duree_max_pret"));
                yield fc;
            }
            case TOUR_DE_ROLE -> {
                TontineTourDeRole tr = new TontineTourDeRole();
                String ordre = rs.getString("ordre_tour");
                if (ordre != null && !ordre.isBlank()) {
                    tr.setOrdreTour(new ArrayList<>(List.of(ordre.split(","))));
                }
                tr.setTourActuelIndex(rs.getInt("tour_actuel_index"));
                yield tr;
            }
            case TIRAGE_AU_SORT -> new TontineTirageAuSort();
        };

        t.setId(rs.getString("id"));
        t.setNom(rs.getString("nom"));
        t.setDescription(rs.getString("description"));
        t.setFrequence(freq);
        t.setDureeLabel(rs.getString("duree_label"));
        t.setMontantCotisation(rs.getBigDecimal("montant_cotisation"));
        t.setMontantMin(rs.getBigDecimal("montant_min"));
        t.setMontantMax(rs.getBigDecimal("montant_max"));
        t.setPenaliteRetard(rs.getBigDecimal("penalite_retard"));
        t.setNbMembresMax(rs.getInt("nb_membres_max"));
        t.setGerantId(rs.getString("gerant_id"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) t.setDateCreation(ts.toLocalDateTime());
        return t;
    }
}
