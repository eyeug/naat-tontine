package sn.naat.dao;

import sn.naat.model.DemandeReinitialisation;
import sn.naat.model.Enums.StatutDemande;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DemandeReinitialisationDAO {

    public DemandeReinitialisation creer(DemandeReinitialisation d) throws SQLException {
        String sql = "INSERT INTO demande_reinitialisation_mdp (id, utilisateur_id, statut) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getUtilisateurId());
            ps.setString(3, d.getStatut().name().toLowerCase());
            ps.executeUpdate();
        }
        return d;
    }

    /** Évite d'accumuler des demandes en double pour le même utilisateur. */
    public boolean existeEnAttente(String utilisateurId) throws SQLException {
        String sql = "SELECT 1 FROM demande_reinitialisation_mdp WHERE utilisateur_id = ? AND statut = 'en_attente' LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<DemandeReinitialisation> listerEnAttente() throws SQLException {
        List<DemandeReinitialisation> resultats = new ArrayList<>();
        String sql = "SELECT * FROM demande_reinitialisation_mdp WHERE statut = 'en_attente' ORDER BY date_envoi";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    public Optional<DemandeReinitialisation> trouverParId(String id) throws SQLException {
        String sql = "SELECT * FROM demande_reinitialisation_mdp WHERE id = ?";
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
     * retourne 0 si elle a déjà été traitée, ce qui empêche deux administrateurs de générer chacun
     * un mot de passe pour la même demande (le second écraserait celui que le premier a communiqué).
     */
    public int changerStatut(String id, StatutDemande statut) throws SQLException {
        String sql = "UPDATE demande_reinitialisation_mdp SET statut = ?, date_traitement = CURRENT_TIMESTAMP "
                   + "WHERE id = ? AND statut = 'en_attente'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, id);
            return ps.executeUpdate();
        }
    }

    private DemandeReinitialisation mapper(ResultSet rs) throws SQLException {
        DemandeReinitialisation d = new DemandeReinitialisation();
        d.setId(rs.getString("id"));
        d.setUtilisateurId(rs.getString("utilisateur_id"));
        Timestamp envoi = rs.getTimestamp("date_envoi");
        if (envoi != null) d.setDateEnvoi(envoi.toLocalDateTime());
        d.setStatut(StatutDemande.valueOf(rs.getString("statut").toUpperCase()));
        Timestamp traitement = rs.getTimestamp("date_traitement");
        if (traitement != null) d.setDateTraitement(traitement.toLocalDateTime());
        return d;
    }
}
