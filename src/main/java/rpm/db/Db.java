package rpm.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {

    public static Connection getConnection() throws SQLException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found on classpath", e);
        }

        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String db   = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        String pass = System.getenv("PGPASSWORD");

        if (host == null || port == null || db == null || user == null || pass == null) {
            throw new SQLException("Missing PG* env vars. Is Postgres service bound to this app?");
        }

        // Cloud-friendly JDBC URL: SSL + timeouts to avoid hanging / vague failures
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db
                + "?sslmode=require"
                + "&connectTimeout=5"
                + "&socketTimeout=10"
                + "&tcpKeepAlive=true";

        return DriverManager.getConnection(url, user, pass);
    }
}
