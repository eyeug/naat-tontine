package sn.naat.dao;

import sn.naat.model.Enums.StatutTransaction;
import sn.naat.model.Enums.TypeTransaction;
import sn.naat.model.Transaction;
import sn.naat.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public Transaction creer(Transaction t) throws SQLException {
        String sql = """
            INSERT INTO transaction_tontine
                (id, tontine_id, membre_id, distribution_id, mode_paiement_id, type, montant, statut, libelle)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getTontineId());
            ps.setString(3, t.getMembreId());
            ps.setString(4, t.getDistributionId());
            ps.setString(5, t.getModePaiementId());
            ps.setString(6, t.getType().name().toLowerCase());
            ps.setBigDecimal(7, t.getMontant());
            ps.setString(8, t.getStatut().name().toLowerCase());
            ps.setString(9, t.getLibelle());
            ps.executeUpdate();
        }
        return t;
    }

    public List<Transaction> listerParTontine(String tontineId) throws SQLException {
        return lister("SELECT * FROM transaction_tontine WHERE tontine_id = ? ORDER BY date DESC", tontineId);
    }

    public List<Transaction> listerParMembre(String membreId) throws SQLException {
        return lister("SELECT * FROM transaction_tontine WHERE membre_id = ? ORDER BY date DESC", membreId);
    }

    /**
     * Membres ayant reçu au moins un versement (une distribution) sur cette tontine,
     * DANS UNE SÉRIE DONNÉE. Un membre peut être payé une fois par série : après une
     * nouvelle série, il redevient « pas encore servi ». La série d'un versement est
     * celle du cycle de sa distribution (versement -> distribution -> cycle).
     */
    public java.util.Set<String> membresAyantRecuVersement(String tontineId, int serie) throws SQLException {
        java.util.Set<String> ids = new java.util.HashSet<>();
        String sql = """
            SELECT DISTINCT t.membre_id
            FROM transaction_tontine t
            JOIN distribution d ON d.id = t.distribution_id
            JOIN cycle c ON c.id = d.cycle_id
            WHERE t.tontine_id = ? AND t.type = 'versement' AND c.serie = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setInt(2, serie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("membre_id"));
            }
        }
        return ids;
    }

    /** Historique d'un membre limité à une tontine précise (vue Gérant). */
    public List<Transaction> listerParTontineEtMembre(String tontineId, String membreId) throws SQLException {
        List<Transaction> resultats = new ArrayList<>();
        String sql = "SELECT * FROM transaction_tontine WHERE tontine_id = ? AND membre_id = ? ORDER BY date DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setString(2, membreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    /**
     * Somme des cotisations d'une tontine sur une période [depuis, jusqua[
     * (base du calcul d'une Distribution). jusqua borne explicitement la
     * fin du cycle — sans cette borne, un cycle mal clôturé continuerait
     * d'aspirer les cotisations des cycles suivants.
     */
    public BigDecimal sommeCotisationsDepuis(String tontineId, LocalDateTime depuis, LocalDateTime jusqua) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(montant), 0) AS total FROM transaction_tontine
            WHERE tontine_id = ? AND type = 'cotisation' AND date >= ? AND date < ?
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

    /** Un membre a-t-il déjà cotisé sur cette tontine depuis une date donnée (= début du cycle en cours) ? */
    public boolean aDejaCotiseDepuis(String tontineId, String membreId, LocalDateTime depuis) throws SQLException {
        String sql = """
            SELECT 1 FROM transaction_tontine
            WHERE tontine_id = ? AND membre_id = ? AND type = 'cotisation' AND date >= ?
            LIMIT 1
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setString(2, membreId);
            ps.setTimestamp(3, Timestamp.valueOf(depuis));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Somme cotisée par un membre précis depuis une date donnée (base de la répartition proportionnelle Fonds commun). */
    public BigDecimal sommeCotisationsMembreDepuis(String tontineId, String membreId, LocalDateTime depuis) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(montant), 0) AS total FROM transaction_tontine
            WHERE tontine_id = ? AND membre_id = ? AND type = 'cotisation' AND date >= ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setString(2, membreId);
            ps.setTimestamp(3, Timestamp.valueOf(depuis));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("total");
            }
        }
        return BigDecimal.ZERO;
    }

    private List<Transaction> lister(String sql, String param) throws SQLException {
        List<Transaction> resultats = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    private Transaction mapper(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getString("id"));
        t.setTontineId(rs.getString("tontine_id"));
        t.setMembreId(rs.getString("membre_id"));
        t.setDistributionId(rs.getString("distribution_id"));
        t.setModePaiementId(rs.getString("mode_paiement_id"));
        t.setType(TypeTransaction.valueOf(rs.getString("type").toUpperCase()));
        t.setMontant(rs.getBigDecimal("montant"));
        Timestamp ts = rs.getTimestamp("date");
        if (ts != null) t.setDate(ts.toLocalDateTime());
        t.setStatut(StatutTransaction.valueOf(rs.getString("statut").toUpperCase()));
        t.setLibelle(rs.getString("libelle"));
        return t;
    }
}
