package sn.naat.dao;

import sn.naat.model.JournalAudit;
import sn.naat.model.Notification;
import sn.naat.model.Tirage;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Regroupe les DAO des entités "légères" : Tirage, Notification, JournalAudit. */
public class DiversDAO {

    // --- Tirage ---

    public Tirage creerTirage(Tirage t) throws SQLException {
        String sql = "INSERT INTO tirage (id, tontine_id, semaine, serie, date, beneficiaire_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getTontineId());
            ps.setInt(3, t.getSemaine());
            ps.setInt(4, t.getSerie());
            ps.setDate(5, Date.valueOf(t.getDate()));
            ps.setString(6, t.getBeneficiaireId());
            ps.executeUpdate();
        }
        return t;
    }

    public List<Tirage> listerParTontine(String tontineId) throws SQLException {
        List<Tirage> resultats = new ArrayList<>();
        String sql = "SELECT * FROM tirage WHERE tontine_id = ? ORDER BY semaine";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tirage t = new Tirage();
                    t.setId(rs.getString("id"));
                    t.setTontineId(rs.getString("tontine_id"));
                    t.setSemaine(rs.getInt("semaine"));
                    t.setSerie(rs.getInt("serie"));
                    t.setDate(rs.getDate("date").toLocalDate());
                    t.setBeneficiaireId(rs.getString("beneficiaire_id"));
                    resultats.add(t);
                }
            }
        }
        return resultats;
    }

    // --- Notification ---

    public Notification creerNotification(Notification n) throws SQLException {
        String sql = "INSERT INTO notification (id, utilisateur_id, message, type, ref_id, lue) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getId());
            ps.setString(2, n.getUtilisateurId());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType());
            ps.setString(5, n.getRefId());
            ps.setBoolean(6, n.isLue());
            ps.executeUpdate();
        }
        return n;
    }

    public List<Notification> listerParUtilisateur(String utilisateurId) throws SQLException {
        List<Notification> resultats = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE utilisateur_id = ? ORDER BY date_envoi DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getString("id"));
                    n.setUtilisateurId(rs.getString("utilisateur_id"));
                    n.setMessage(rs.getString("message"));
                    Timestamp ts = rs.getTimestamp("date_envoi");
                    if (ts != null) n.setDateEnvoi(ts.toLocalDateTime());
                    n.setType(rs.getString("type"));
                    n.setRefId(rs.getString("ref_id"));
                    n.setLue(rs.getBoolean("lue"));
                    resultats.add(n);
                }
            }
        }
        return resultats;
    }

    // --- JournalAudit ---

    public JournalAudit enregistrer(JournalAudit entry) throws SQLException {
        String sql = "INSERT INTO journal_audit (id, acteur_id, action) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entry.getId());
            ps.setString(2, entry.getActeurId());
            ps.setString(3, entry.getAction());
            ps.executeUpdate();
        }
        return entry;
    }

    public List<JournalAudit> listerTout() throws SQLException {
        List<JournalAudit> resultats = new ArrayList<>();
        String sql = "SELECT * FROM journal_audit ORDER BY ts DESC LIMIT 200";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JournalAudit j = new JournalAudit();
                j.setId(rs.getString("id"));
                Timestamp ts = rs.getTimestamp("ts");
                if (ts != null) j.setTs(ts.toLocalDateTime());
                j.setActeurId(rs.getString("acteur_id"));
                j.setAction(rs.getString("action"));
                resultats.add(j);
            }
        }
        return resultats;
    }
}
