package sn.naat.dao;

import sn.naat.model.DemandeNouvelleSerie;
import sn.naat.model.Enums.StatutDemande;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DemandeNouvelleSerieDAO {

    public DemandeNouvelleSerie creer(DemandeNouvelleSerie d) throws SQLException {
        String sql = "INSERT INTO demande_nouvelle_serie (id, tontine_id, gerant_id, statut) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getTontineId());
            ps.setString(3, d.getGerantId());
            ps.setString(4, d.getStatut().name().toLowerCase());
            ps.executeUpdate();
        }
        return d;
    }

    public boolean existeEnAttente(String tontineId) throws SQLException {
        String sql = "SELECT 1 FROM demande_nouvelle_serie WHERE tontine_id = ? AND statut = 'en_attente' LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Nombre de nouvelles séries déjà validées : la série courante d'une tontine vaut 1 + ce nombre. */
    public int compterValidees(String tontineId) throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM demande_nouvelle_serie WHERE tontine_id = ? AND statut = 'validee'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("nb") : 0;
            }
        }
    }

    public List<DemandeNouvelleSerie> listerEnAttente() throws SQLException {
        List<DemandeNouvelleSerie> resultats = new ArrayList<>();
        String sql = "SELECT * FROM demande_nouvelle_serie WHERE statut = 'en_attente' ORDER BY date_envoi";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    public Optional<DemandeNouvelleSerie> trouverParId(String id) throws SQLException {
        String sql = "SELECT * FROM demande_nouvelle_serie WHERE id = ?";
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
     * Enregistre la décision de l'administrateur. Ne modifie QUE une demande encore en attente :
     * retourne 0 si elle a déjà été traitée, ce qui empêche de valider deux fois la même demande
     * (et donc de faire avancer la série deux fois).
     */
    public int changerStatut(String id, StatutDemande statut) throws SQLException {
        String sql = "UPDATE demande_nouvelle_serie SET statut = ?, date_decision = CURRENT_TIMESTAMP "
                   + "WHERE id = ? AND statut = 'en_attente'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, id);
            return ps.executeUpdate();
        }
    }

    private DemandeNouvelleSerie mapper(ResultSet rs) throws SQLException {
        DemandeNouvelleSerie d = new DemandeNouvelleSerie();
        d.setId(rs.getString("id"));
        d.setTontineId(rs.getString("tontine_id"));
        d.setGerantId(rs.getString("gerant_id"));
        Timestamp envoi = rs.getTimestamp("date_envoi");
        if (envoi != null) d.setDateEnvoi(envoi.toLocalDateTime());
        d.setStatut(StatutDemande.valueOf(rs.getString("statut").toUpperCase()));
        Timestamp decision = rs.getTimestamp("date_decision");
        if (decision != null) d.setDateDecision(decision.toLocalDateTime());
        return d;
    }
}
