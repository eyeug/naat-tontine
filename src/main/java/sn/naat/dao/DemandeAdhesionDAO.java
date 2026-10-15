package sn.naat.dao;

import sn.naat.model.DemandeAdhesion;
import sn.naat.model.Enums.StatutDemande;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DemandeAdhesionDAO {

    public DemandeAdhesion creer(DemandeAdhesion d) throws SQLException {
        String sql = """
            INSERT INTO demande_adhesion (id, tontine_id, nom, tel, email, mot_de_passe_hash,
                piece_identite_hash, certificat_residence_hash, cgu_acceptees, statut, contact_ref)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getTontineId());
            ps.setString(3, d.getNom());
            ps.setString(4, d.getTel());
            ps.setString(5, d.getEmail());
            ps.setString(6, d.getMotDePasseHash());
            ps.setString(7, d.getPieceIdentiteHash());
            ps.setString(8, d.getCertificatResidenceHash());
            ps.setBoolean(9, d.isCguAcceptees());
            ps.setString(10, d.getStatut().name().toLowerCase());
            ps.setString(11, d.getContactRef());
            ps.executeUpdate();
        }
        return d;
    }

    public List<DemandeAdhesion> listerEnAttenteParTontine(String tontineId) throws SQLException {
        List<DemandeAdhesion> resultats = new ArrayList<>();
        String sql = "SELECT * FROM demande_adhesion WHERE tontine_id = ? AND statut = 'en_attente' ORDER BY date_envoi";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    /** +validerDemandeAdhesion(demande, statut): Adhesion */
    public void changerStatut(String demandeId, StatutDemande statut) throws SQLException {
        String sql = "UPDATE demande_adhesion SET statut = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, demandeId);
            ps.executeUpdate();
        }
    }

    private DemandeAdhesion mapper(ResultSet rs) throws SQLException {
        DemandeAdhesion d = new DemandeAdhesion();
        d.setId(rs.getString("id"));
        d.setTontineId(rs.getString("tontine_id"));
        d.setNom(rs.getString("nom"));
        d.setTel(rs.getString("tel"));
        d.setEmail(rs.getString("email"));
        d.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        d.setPieceIdentiteHash(rs.getString("piece_identite_hash"));
        d.setCertificatResidenceHash(rs.getString("certificat_residence_hash"));
        d.setCguAcceptees(rs.getBoolean("cgu_acceptees"));
        Timestamp ts = rs.getTimestamp("date_envoi");
        if (ts != null) d.setDateEnvoi(ts.toLocalDateTime());
        d.setStatut(StatutDemande.valueOf(rs.getString("statut").toUpperCase()));
        d.setContactRef(rs.getString("contact_ref"));
        return d;
    }
}
