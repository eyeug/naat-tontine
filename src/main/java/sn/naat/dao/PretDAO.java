package sn.naat.dao;

import sn.naat.model.Enums.StatutPret;
import sn.naat.model.Enums.Valideur;
import sn.naat.model.Pret;
import sn.naat.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PretDAO {

    public Pret creer(Pret p) throws SQLException {
        String sql = """
            INSERT INTO pret (id, tontine_id, membre_id, montant, duree_jours, motif, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getTontineId());
            ps.setString(3, p.getMembreId());
            ps.setBigDecimal(4, p.getMontant());
            ps.setInt(5, p.getDureeJours());
            ps.setString(6, p.getMotif());
            ps.setString(7, p.getStatut().name().toLowerCase());
            ps.executeUpdate();
        }
        return p;
    }

    /** +validerPret(pret, tontine, statut) / +validerPretGerant(...) — mise à jour du statut. */
    public void changerStatut(String pretId, StatutPret statut, Valideur valideur) throws SQLException {
        String sql = "UPDATE pret SET statut = ?, valideur = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, valideur == null ? null : valideur.name().toLowerCase());
            ps.setString(3, pretId);
            ps.executeUpdate();
        }
    }

    public List<Pret> listerEnAttenteParTontine(String tontineId) throws SQLException {
        return lister("SELECT * FROM pret WHERE tontine_id = ? AND statut = 'en_attente' ORDER BY date_demande", tontineId);
    }

    public List<Pret> listerParMembre(String membreId) throws SQLException {
        return lister("SELECT * FROM pret WHERE membre_id = ? ORDER BY date_demande DESC", membreId);
    }

    /** Enregistre le remboursement (capital + intérêt) d'un prêt validé. */
    public void rembourser(String pretId, BigDecimal montantRembourse) throws SQLException {
        String sql = """
            UPDATE pret SET statut = 'rembourse', montant_rembourse = ?, date_remboursement = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, montantRembourse);
            ps.setString(2, pretId);
            ps.executeUpdate();
        }
    }

    /** Prêts d'une tontine pas encore soldés : en attente de décision, ou validés et pas encore remboursés. */
    public int compterNonSoldes(String tontineId) throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM pret WHERE tontine_id = ? AND statut IN ('en_attente', 'valide')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("nb") : 0;
            }
        }
    }

    /** Somme des intérêts (montant_rembourse - montant) perçus sur les prêts remboursés sur [depuis, jusqua[. */
    public BigDecimal sommeInteretsRembourses(String tontineId, LocalDateTime depuis, LocalDateTime jusqua) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(montant_rembourse - montant), 0) AS total FROM pret
            WHERE tontine_id = ? AND statut = 'rembourse' AND date_remboursement >= ? AND date_remboursement < ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setTimestamp(2, Timestamp.valueOf(depuis));
            ps.setTimestamp(3, Timestamp.valueOf(jusqua));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("total");
            }
        }
        return BigDecimal.ZERO;
    }

    public List<Pret> listerTousEnAttente() throws SQLException {
        List<Pret> resultats = new ArrayList<>();
        String sql = "SELECT * FROM pret WHERE statut = 'en_attente' ORDER BY date_demande";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    /** Prêts "valide" d'une tontine — base du calcul des intérêts à répartir (Fonds commun). */
    public List<Pret> listerValidesParTontine(String tontineId) throws SQLException {
        return lister("SELECT * FROM pret WHERE tontine_id = ? AND statut = 'valide'", tontineId);
    }

    /**
     * Prêts en attente demandés par un gérant sur sa propre tontine — ceux
     * que le gérant ne peut pas valider lui-même (règle anti-conflit) et qui
     * doivent donc être traités par un administrateur.
     */
    public List<Pret> listerPretsGerantsEnAttente() throws SQLException {
        List<Pret> resultats = new ArrayList<>();
        String sql = """
            SELECT p.* FROM pret p
            JOIN tontine t ON t.id = p.tontine_id
            WHERE p.statut = 'en_attente' AND p.membre_id = t.gerant_id
            ORDER BY p.date_demande
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    private List<Pret> lister(String sql, String param) throws SQLException {
        List<Pret> resultats = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    private Pret mapper(ResultSet rs) throws SQLException {
        Pret p = new Pret();
        p.setId(rs.getString("id"));
        p.setTontineId(rs.getString("tontine_id"));
        p.setMembreId(rs.getString("membre_id"));
        p.setMontant(rs.getBigDecimal("montant"));
        p.setDureeJours(rs.getInt("duree_jours"));
        p.setMotif(rs.getString("motif"));
        Timestamp ts = rs.getTimestamp("date_demande");
        if (ts != null) p.setDateDemande(ts.toLocalDateTime());
        p.setStatut(StatutPret.valueOf(rs.getString("statut").toUpperCase()));
        String val = rs.getString("valideur");
        if (val != null) p.setValideur(Valideur.valueOf(val.toUpperCase()));
        p.setMontantRembourse(rs.getBigDecimal("montant_rembourse"));
        Timestamp tsRemb = rs.getTimestamp("date_remboursement");
        if (tsRemb != null) p.setDateRemboursement(tsRemb.toLocalDateTime());
        return p;
    }
}
