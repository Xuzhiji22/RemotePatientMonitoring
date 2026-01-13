package rpm.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class Db {

    private static volatile HikariDataSource ds;

    private Db() {}

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    private static HikariDataSource dataSource() throws SQLException {
        if (ds != null) return ds;

        synchronized (Db.class) {
            if (ds != null) return ds;

            if (!hasPgEnv()) {
                throw new SQLException("Missing PG* env vars. Is Postgres service bound to this app?");
            }

            String host = System.getenv("PGHOST");
            String port = System.getenv("PGPORT");
            String db   = System.getenv("PGDATABASE");
            String user = System.getenv("PGUSER");
            String pass = System.getenv("PGPASSWORD");

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db
                    + "?sslmode=require"
                    + "&connectTimeout=5"
                    + "&socketTimeout=10"
                    + "&tcpKeepAlive=true";

            HikariConfig cfg = new HikariConfig();
            cfg.setDriverClassName("org.postgresql.Driver");

            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(user);
            cfg.setPassword(pass);

            cfg.setMaximumPoolSize(5);
            cfg.setMinimumIdle(0);

            cfg.setConnectionTimeout(5000);
            cfg.setValidationTimeout(3000);
            cfg.setIdleTimeout(60_000);
            cfg.setMaxLifetime(5 * 60_000);
            cfg.setKeepaliveTime(30_000);


            cfg.addDataSourceProperty("ApplicationName", "bioeng-rpm-app");

            ds = new HikariDataSource(cfg);
            return ds;
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }


    public static void close() {
        HikariDataSource tmp = ds;
        ds = null;
        if (tmp != null) tmp.close();
    }
}
