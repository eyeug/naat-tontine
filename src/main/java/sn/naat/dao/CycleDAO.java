package sn.naat.dao;

import sn.naat.model.Cycle;
import sn.naat.model.Distribution;
import sn.naat.model.Enums.StatutCycle;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CycleDAO {

    public Cycle creer(Cycle cycle) throws SQLException {
        String sql = "INSERT INTO cycle (id, tontine_id, numero, serie, date_debut, statut) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cycle.getId());
            ps.setString(2, cycle.getTontineId());
            ps.setInt(3, cycle.getNumero());
            ps.setInt(4, cycle.getSerie());
            ps.setDate(5, Date.valueOf(cycle.getDateDebut()));
            ps.setString(6, cycle.getStatut().name().toLowerCase());
            ps.executeUpdate();
        }
        return cycle;
    }

    public Optional<Cycle> trouverCycleEnCours(String tontineId) throws SQLException {
        String sql = "SELECT * FROM cycle WHERE tontine_id = ? AND statut = 'en_cours' ORDER BY numero DESC LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper(rs));
            }
        }
        return Optional.empty();
    }

    public List<Cycle> listerParTontine(String tontineId) throws SQLException {
        List<Cycle> resultats = new ArrayList<>();
        String sql = "SELECT * FROM cycle WHERE tontine_id = ? ORDER BY numero DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    /** +cloturer(): Distribution */
    public void cloturer(String cycleId) throws SQLException {
        String sql = "UPDATE cycle SET statut = 'cloture', date_fin = CURRENT_DATE WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cycleId);
            ps.executeUpdate();
        }
    }

    public Distribution creerDistribution(Distribution d) throws SQLException {
        String sql = "INSERT INTO distribution (id, cycle_id, montant_total, beneficiaire_id) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getCycleId());
            ps.setBigDecimal(3, d.getMontantTotal());
            ps.setString(4, d.getBeneficiaireId());
            ps.executeUpdate();
        }
        return d;
    }

    /** Historique des distributions d'une tontine (toutes cycles confondus), du plus récent au plus ancien. */
    public List<Distribution> listerDistributionsParTontine(String tontineId) throws SQLException {
        List<Distribution> resultats = new ArrayList<>();
        String sql = """
            SELECT d.* FROM distribution d
            JOIN cycle c ON c.id = d.cycle_id
            WHERE c.tontine_id = ?
            ORDER BY d.date DESC
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Distribution d = new Distribution();
                    d.setId(rs.getString("id"));
                    d.setCycleId(rs.getString("cycle_id"));
                    Timestamp ts = rs.getTimestamp("date");
                    if (ts != null) d.setDate(ts.toLocalDateTime());
                    d.setMontantTotal(rs.getBigDecimal("montant_total"));
                    d.setBeneficiaireId(rs.getString("beneficiaire_id"));
                    resultats.add(d);
                }
            }
        }
        return resultats;
    }

    private Cycle mapper(ResultSet rs) throws SQLException {
        Cycle c = new Cycle();
        c.setId(rs.getString("id"));
        c.setTontineId(rs.getString("tontine_id"));
        c.setNumero(rs.getInt("numero"));
        c.setSerie(rs.getInt("serie"));
        Date dd = rs.getDate("date_debut");
        if (dd != null) c.setDateDebut(dd.toLocalDate());
        Date df = rs.getDate("date_fin");
        if (df != null) c.setDateFin(df.toLocalDate());
        c.setStatut(StatutCycle.valueOf(rs.getString("statut").toUpperCase()));
        return c;
    }
}
