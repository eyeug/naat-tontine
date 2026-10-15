package sn.naat.dao;

import sn.naat.model.*;
import sn.naat.model.Enums.StatutCompte;
import sn.naat.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour la hiérarchie Utilisateur / Membre / Gerant / Administrateur,
 * mappée en "table par classe" : utilisateur, membre, gerant,
 * administrateur (chacune avec sa propre table, cf. naat_schema.sql).
 */
public class UtilisateurDAO {

    public Utilisateur creerUtilisateur(Utilisateur u) throws SQLException {
        String sql = """
            INSERT INTO utilisateur (id, nom, initials, tel, email, mot_de_passe_hash, statut_compte)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getId());
            ps.setString(2, u.getNom());
            ps.setString(3, u.getInitials());
            ps.setString(4, u.getTel());
            ps.setString(5, u.getEmail());
            ps.setString(6, u.getMotDePasseHash());
            ps.setString(7, u.getStatutCompte().name().toLowerCase());
            ps.executeUpdate();
        }
        return u;
    }

    /** Promeut un utilisateur en Membre (crée la ligne dans la table membre si absente). */
    public void promouvoirMembre(String utilisateurId) throws SQLException {
        String sql = "INSERT IGNORE INTO membre (utilisateur_id) VALUES (?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    /** Promeut un membre en Gérant (Membre &lt;|-- Gerant : il doit déjà être membre). */
    public void promouvoirGerant(String utilisateurId) throws SQLException {
        promouvoirMembre(utilisateurId);
        String sql = "INSERT IGNORE INTO gerant (utilisateur_id) VALUES (?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    /** Promeut un utilisateur en Administrateur (Utilisateur &lt;|-- Administrateur, directement). */
    public void promouvoirAdministrateur(String utilisateurId) throws SQLException {
        String sql = "INSERT IGNORE INTO administrateur (utilisateur_id) VALUES (?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    public boolean estMembre(String utilisateurId) throws SQLException {
        return existeDans("membre", utilisateurId);
    }

    public boolean estGerant(String utilisateurId) throws SQLException {
        return existeDans("gerant", utilisateurId);
    }

    public boolean estAdministrateur(String utilisateurId) throws SQLException {
        return existeDans("administrateur", utilisateurId);
    }

    private boolean existeDans(String table, String utilisateurId) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE utilisateur_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Met à jour le téléphone et l'email, et, si nouveauHash n'est pas null, le mot de passe
     * (qui n'est alors plus provisoire). Une seule requête : tout est enregistré, ou rien.
     */
    public void mettreAJourProfil(String id, String tel, String email, String nouveauHash) throws SQLException {
        String sql = nouveauHash == null
                ? "UPDATE utilisateur SET tel = ?, email = ? WHERE id = ?"
                : "UPDATE utilisateur SET tel = ?, email = ?, mot_de_passe_hash = ?, doit_changer_mdp = FALSE WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tel);
            ps.setString(2, email);
            if (nouveauHash == null) {
                ps.setString(3, id);
            } else {
                ps.setString(3, nouveauHash);
                ps.setString(4, id);
            }
            ps.executeUpdate();
        }
    }

    /** Remplace le mot de passe ; doitChanger vrai pour un mot de passe provisoire remis par l'administrateur. */
    public void definirMotDePasse(String id, String hash, boolean doitChanger) throws SQLException {
        String sql = "UPDATE utilisateur SET mot_de_passe_hash = ?, doit_changer_mdp = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setBoolean(2, doitChanger);
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    /** Retire le rôle d'administrateur (le compte lui-même et ses autres rôles ne sont pas touchés). */
    public void retirerAdministrateur(String utilisateurId) throws SQLException {
        String sql = "DELETE FROM administrateur WHERE utilisateur_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    /** Nombre de comptes administrateurs, suspendus compris (pour la limite du nombre d'administrateurs). */
    public int compterAdministrateurs() throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM administrateur";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("nb") : 0;
        }
    }

    /** Nombre d'administrateurs dont le compte est actif (pour ne jamais suspendre le dernier). */
    public int compterAdministrateursActifs() throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM administrateur a "
                   + "JOIN utilisateur u ON u.id = a.utilisateur_id WHERE u.statut_compte = 'actif'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("nb") : 0;
        }
    }

    /** Ids de tous les administrateurs (pour leur envoyer une notification). */
    public List<String> listerAdministrateursIds() throws SQLException {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT utilisateur_id FROM administrateur";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getString("utilisateur_id"));
        }
        return ids;
    }

    public Optional<Utilisateur> trouverParTelephone(String tel) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE tel = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Utilisateur> trouverParId(String id) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper(rs));
            }
        }
        return Optional.empty();
    }

    public List<Utilisateur> listerTous() throws SQLException {
        List<Utilisateur> resultats = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur ORDER BY nom";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultats.add(mapper(rs));
        }
        return resultats;
    }

    public void suspendreCompte(String utilisateurId) throws SQLException {
        changerStatut(utilisateurId, StatutCompte.SUSPENDU);
    }

    public void reactiverCompte(String utilisateurId) throws SQLException {
        changerStatut(utilisateurId, StatutCompte.ACTIF);
    }

    private void changerStatut(String utilisateurId, StatutCompte statut) throws SQLException {
        String sql = "UPDATE utilisateur SET statut_compte = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name().toLowerCase());
            ps.setString(2, utilisateurId);
            ps.executeUpdate();
        }
    }

    private Utilisateur mapper(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getString("id"));
        u.setNom(rs.getString("nom"));
        u.setInitials(rs.getString("initials"));
        u.setTel(rs.getString("tel"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        u.setStatutCompte(StatutCompte.valueOf(rs.getString("statut_compte").toUpperCase()));
        u.setDoitChangerMotDePasse(rs.getBoolean("doit_changer_mdp"));
        Timestamp ts = rs.getTimestamp("date_inscription");
        if (ts != null) u.setDateInscription(ts.toLocalDateTime());
        return u;
    }
}
