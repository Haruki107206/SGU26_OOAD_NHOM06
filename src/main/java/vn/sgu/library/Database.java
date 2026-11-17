package vn.sgu.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Shared database connection settings for the standalone library module. */
public final class Database {
    private Database() { }

    public static Connection openConnection() throws SQLException {
        String url = System.getenv().getOrDefault("LIBRARY_DB_URL",
                "jdbc:mysql://localhost:3306/sgu_library?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC");
        String user = System.getenv().getOrDefault("LIBRARY_DB_USER", "root");
        String password = System.getenv().getOrDefault("LIBRARY_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
