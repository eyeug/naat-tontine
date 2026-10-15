package sn.naat.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC centralisée vers la base MySQL "naat".
 * Adapter URL / UTILISATEUR / MOT_DE_PASSE à votre environnement,
 * ou surcharger via les variables d'environnement NAAT_DB_URL,
 * NAAT_DB_USER, NAAT_DB_PASSWORD.
 */
public final class DatabaseConnection {

    private static final String URL = System.getenv().getOrDefault(
            "NAAT_DB_URL", "jdbc:mysql://localhost:3306/naat?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("NAAT_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("NAAT_DB_PASSWORD", "");

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
