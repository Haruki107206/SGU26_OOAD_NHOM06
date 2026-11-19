package vn.sgu.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Shared database connection settings for the standalone library module. */
public final class Database {
    private Database() { }

    public static Connection openConnection() throws SQLException {
        String url = System.getenv().getOrDefault("LIBRARY_DB_URL",
                "jdbc:sqlserver://localhost:1433;databaseName=sgu_library;encrypt=true;trustServerCertificate=true");
        String user = System.getenv().getOrDefault("LIBRARY_DB_USER", "library_app");
        String password = System.getenv().getOrDefault("LIBRARY_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
