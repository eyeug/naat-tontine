package sn.naat.dao;

import sn.naat.model.ModePaiement;
import sn.naat.util.DatabaseConnection;

import java.sql.*;

public class ModePaiementDAO {

    public ModePaiement enregistrer(ModePaiement mode) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mode_paiement (id, reference, statut) VALUES (?, ?, ?)")) {
                    ps.setString(1, mode.getId());
                    ps.setString(2, mode.getReference());
                    ps.setString(3, mode.getStatut().name().toLowerCase());
                    ps.executeUpdate();
                }

                if (mode instanceof ModePaiement.Wave wave) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO wave (mode_paiement_id, numero_wave) VALUES (?, ?)")) {
                        ps.setString(1, wave.getId());
                        ps.setString(2, wave.getNumeroWave());
                        ps.executeUpdate();
                    }
                } else if (mode instanceof ModePaiement.OrangeMoney om) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO orange_money (mode_paiement_id, numero_orange_money) VALUES (?, ?)")) {
                        ps.setString(1, om.getId());
                        ps.setString(2, om.getNumeroOrangeMoney());
                        ps.executeUpdate();
                    }
                } else if (mode instanceof ModePaiement.Especes) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO especes (mode_paiement_id) VALUES (?)")) {
                        ps.setString(1, mode.getId());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
        return mode;
    }
}
