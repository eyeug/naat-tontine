package sn.naat.dao;

import sn.naat.model.Adhesion;
import sn.naat.model.Enums.RoleAdhesion;
import sn.naat.model.Enums.StatutAdhesion;
import sn.naat.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdhesionDAO {

    public Adhesion creer(Adhesion a) throws SQLException {
        String sql = """
            INSERT INTO adhesion (id, tontine_id, membre_id, role, solde, total_cotise, cotisations_faites, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getTontineId());
            ps.setString(3, a.getMembreId());
            ps.setString(4, a.getRole().name().toLowerCase());
            ps.setBigDecimal(5, a.getSolde());
            ps.setBigDecimal(6, a.getTotalCotise());
            ps.setInt(7, a.getCotisationsFaites());
            ps.setString(8, a.getStatut().name().toLowerCase());
            ps.executeUpdate();
        }
        return a;
    }

    public Optional<Adhesion> trouver(String tontineId, String membreId) throws SQLException {
        String sql = "SELECT * FROM adhesion WHERE tontine_id = ? AND membre_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            ps.setString(2, membreId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper(rs));
            }
        }
        return Optional.empty();
    }

    public List<Adhesion> listerParTontine(String tontineId) throws SQLException {
        List<Adhesion> resultats = new ArrayList<>();
        String sql = "SELECT * FROM adhesion WHERE tontine_id = ? ORDER BY date_adhesion";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultats.add(mapper(rs));
            }
        }
        return resultats;
    }

    /** -mettreAJourSolde() + enregistrerCotisation() côté objet métier ; ici la persistance. */
    public void enregistrerCotisation(String adhesionId, BigDecimal montant) throws SQLException {
        String sql = """
            UPDATE adhesion
            SET solde = solde + ?, total_cotise = total_cotise + ?, cotisations_faites = cotisations_faites + 1
            WHERE id = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, montant);
            ps.setBigDecimal(2, montant);
            ps.setString(3, adhesionId);
            ps.executeUpdate();
        }
    }

    /**
     * Nouvelle série : met fin aux adhésions de l'ancienne série, sauf celle du gérant qui reste et dont
     * les compteurs repartent à zéro. Les anciens membres redemandent à rejoindre s'ils le souhaitent.
     * L'historique financier n'est pas perdu : il reste dans les transactions, distributions et tirages.
     * Les deux requêtes forment une seule transaction : si l'une échoue, rien n'est modifié.
     */
    public void reinitialiserPourNouvelleSerie(String tontineId, String gerantId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement supprimer = c.prepareStatement(
                         "DELETE FROM adhesion WHERE tontine_id = ? AND membre_id <> ?");
                 PreparedStatement remettreAZero = c.prepareStatement(
                         "UPDATE adhesion SET solde = 0, total_cotise = 0, cotisations_faites = 0 "
                       + "WHERE tontine_id = ? AND membre_id = ?")) {
                supprimer.setString(1, tontineId);
                supprimer.setString(2, gerantId);
                supprimer.executeUpdate();
                remettreAZero.setString(1, tontineId);
                remettreAZero.setString(2, gerantId);
                remettreAZero.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Réactive une adhésion désactivée par le gérant. Ne modifie QUE une adhésion désactivée : retourne 0
     * si elle ne l'était pas (déjà réactivée entre-temps, par exemple).
     */
    public int reactiver(String adhesionId) throws SQLException {
        String sql = "UPDATE adhesion SET statut = 'actif' WHERE id = ? AND statut = 'inactif'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adhesionId);
            return ps.executeUpdate();
        }
    }

    /** Nombre de membres actifs (utile pour l'affichage public "X / Y membres"). */
    public int compterMembresActifs(String tontineId) throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM adhesion WHERE tontine_id = ? AND statut = 'actif'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tontineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("nb");
            }
        }
        return 0;
    }

    public void desactiver(String adhesionId) throws SQLException {
        String sql = "UPDATE adhesion SET statut = 'inactif' WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adhesionId);
            ps.executeUpdate();
        }
    }

    /**
     * Applique une pénalité de retard (déduite du solde) à un membre qui n'a
     * pas cotisé avant la clôture du cycle — "une pénalité s'applique
     * automatiquement après la date d'échéance", comme prévu par les règles
     * de la tontine (Tontine.penaliteRetard).
     */
    public void appliquerPenalite(String adhesionId, java.math.BigDecimal montant) throws SQLException {
        String sql = "UPDATE adhesion SET solde = solde - ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, montant);
            ps.setString(2, adhesionId);
            ps.executeUpdate();
        }
    }

    /**
     * Crédite le solde d'un membre — utilisé pour les intérêts distribués
     * en Fonds commun (contrairement à un versement de tour/tirage, qui
     * part directement vers l'extérieur sans transiter par le solde).
     */
    public void crediterSolde(String adhesionId, java.math.BigDecimal montant) throws SQLException {
        String sql = "UPDATE adhesion SET solde = solde + ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, montant);
            ps.setString(2, adhesionId);
            ps.executeUpdate();
        }
    }

    private Adhesion mapper(ResultSet rs) throws SQLException {
        Adhesion a = new Adhesion();
        a.setId(rs.getString("id"));
        a.setTontineId(rs.getString("tontine_id"));
        a.setMembreId(rs.getString("membre_id"));
        a.setRole(RoleAdhesion.valueOf(rs.getString("role").toUpperCase()));
        Timestamp ts = rs.getTimestamp("date_adhesion");
        if (ts != null) a.setDateAdhesion(ts.toLocalDateTime());
        a.setSolde(rs.getBigDecimal("solde"));
        a.setTotalCotise(rs.getBigDecimal("total_cotise"));
        a.setCotisationsFaites(rs.getInt("cotisations_faites"));
        a.setStatut(StatutAdhesion.valueOf(rs.getString("statut").toUpperCase()));
        return a;
    }
}
