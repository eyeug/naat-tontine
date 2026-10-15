package sn.naat.dao;

import sn.naat.model.DemandeCreation;
import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.StatutDemande;
import sn.naat.model.Enums.TypeTontine;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DemandeCreationDAO {

    public DemandeCreation creer(DemandeCreation d) throws SQLException {
        String sql = """
            INSERT INTO demande_creation (id, demandeur_id, nom_souhaite, type_souhaite, montant, frequence, nb_membres, statut, montant_min, montant_max)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getDemandeurId());
            ps.setString(3, d.getNomSouhaite());
            ps.setString(4, d.getTypeSouhaite().name().toLowerCase());
            ps.setBigDecimal(5, d.getMontant());
            ps.setString(6, d.getFrequence().name().toLowerCase());
            ps.setInt(7, d.getNbMembres());
            ps.setString(8, d.getStatut().name().toLowerCase());
            ps.setBigDecimal(9, d.getMontantMin());
            ps.setBigDecimal(10, d.getMontantMax());
            ps.executeUpdate();
        }
        return d;
    }

    /** Nombre de demandes de création de cet utilisateur qui attendent encore la décision de l'administrateur. */
    public int compterEnAttente(String demandeurId) throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM demande_creation WHERE demandeur_id = ? AND statut = 'en_attente'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, demandeurId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("nb") : 0;
            }
        }
    }

    public List<DemandeCreation> listerEnAttente() throws SQLException {
        List<DemandeCreation> resultats = new ArrayList<>();
        String sql = "SELECT * FROM demande_creation WHERE statut = 'en_attente' ORDER BY date_envoi";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    /** +validerDemandeCreation(demande, statut): Tontine */
    public void changerStatut(String demandeId, StatutDemande statut) throws SQLException {
        String sql = "UPDATE demande_creation SET statut = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, demandeId);
            ps.executeUpdate();
        }
    }

    private DemandeCreation mapper(ResultSet rs) throws SQLException {
        DemandeCreation d = new DemandeCreation();
        d.setId(rs.getString("id"));
        d.setDemandeurId(rs.getString("demandeur_id"));
        d.setNomSouhaite(rs.getString("nom_souhaite"));
        d.setTypeSouhaite(TypeTontine.valueOf(rs.getString("type_souhaite").toUpperCase()));
        d.setMontant(rs.getBigDecimal("montant"));
        d.setFrequence(Frequence.valueOf(rs.getString("frequence").toUpperCase()));
        d.setNbMembres(rs.getInt("nb_membres"));
        d.setMontantMin(rs.getBigDecimal("montant_min"));
        d.setMontantMax(rs.getBigDecimal("montant_max"));
        Timestamp ts = rs.getTimestamp("date_envoi");
        if (ts != null) d.setDateEnvoi(ts.toLocalDateTime());
        d.setStatut(StatutDemande.valueOf(rs.getString("statut").toUpperCase()));
        return d;
    }
}
