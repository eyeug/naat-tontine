package sn.naat.dao;

import sn.naat.model.DemandeTourAnticipe;
import sn.naat.model.Enums.StatutDemande;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DemandeTourAnticipeDAO {

    public DemandeTourAnticipe creer(DemandeTourAnticipe d) throws SQLException {
        String sql = "INSERT INTO demande_tour_anticipe (id, tontine_id, membre_id, motif, statut) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getTontineId());
            ps.setString(3, d.getMembreId());
            ps.setString(4, d.getMotif());
            ps.setString(5, d.getStatut().name().toLowerCase());
            ps.executeUpdate();
        }
        return d;
    }

    /** Une seule demande en attente par membre et par tontine. */
    public boolean existeEnAttente(String tontineId, String membreId) throws SQLException {
        String sql = "SELECT 1 FROM demande_tour_anticipe WHERE tontine_id = ? AND membre_id = ? AND statut = 'en_attente' LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setString(2, membreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<DemandeTourAnticipe> listerEnAttente(String tontineId) throws SQLException {
        List<DemandeTourAnticipe> resultats = new ArrayList<>();
        String sql = "SELECT * FROM demande_tour_anticipe WHERE tontine_id = ? AND statut = 'en_attente' ORDER BY date_envoi";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    public Optional<DemandeTourAnticipe> trouverParId(String id) throws SQLException {
        String sql = "SELECT * FROM demande_tour_anticipe WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Enregistre la décision du gérant. Ne modifie QUE une demande encore en attente : retourne 0 si elle a
     * déjà été traitée, ce qui empêche d'accorder deux fois la même demande (et de réordonner le tour deux fois).
     */
    public int changerStatut(String id, StatutDemande statut) throws SQLException {
        String sql = "UPDATE demande_tour_anticipe SET statut = ?, date_decision = CURRENT_TIMESTAMP "
                   + "WHERE id = ? AND statut = 'en_attente'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, id);
            return ps.executeUpdate();
        }
    }

    /** Une nouvelle série repart de zéro : les demandes de l'ancienne série n'ont plus d'objet. */
    public int refuserToutesEnAttente(String tontineId) throws SQLException {
        String sql = "UPDATE demande_tour_anticipe SET statut = 'refusee', date_decision = CURRENT_TIMESTAMP "
                   + "WHERE tontine_id = ? AND statut = 'en_attente'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            return ps.executeUpdate();
        }
    }

    private DemandeTourAnticipe mapper(ResultSet rs) throws SQLException {
        DemandeTourAnticipe d = new DemandeTourAnticipe();
        d.setId(rs.getString("id"));
        d.setTontineId(rs.getString("tontine_id"));
        d.setMembreId(rs.getString("membre_id"));
        d.setMotif(rs.getString("motif"));
        Timestamp envoi = rs.getTimestamp("date_envoi");
        if (envoi != null) d.setDateEnvoi(envoi.toLocalDateTime());
        d.setStatut(StatutDemande.valueOf(rs.getString("statut").toUpperCase()));
        Timestamp decision = rs.getTimestamp("date_decision");
        if (decision != null) d.setDateDecision(decision.toLocalDateTime());
        return d;
    }
}
