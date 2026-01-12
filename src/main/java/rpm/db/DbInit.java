package rpm.db;

import java.sql.Connection;
import java.sql.Statement;

public final class DbInit {

    private DbInit() {}

    public static void init() throws Exception {
        try (Connection c = Db.getConnection();
             Statement st = c.createStatement()) {

            // patients
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS patients (" +
                            "patient_id TEXT PRIMARY KEY," +
                            "name TEXT NOT NULL," +
                            "age INT NOT NULL," +
                            "ward TEXT," +
                            "email TEXT," +
                            "emergency_contact TEXT" +
                            ")"
            );

            // vital_samples
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS vital_samples (" +
                            "patient_id TEXT NOT NULL," +
                            "ts_ms BIGINT NOT NULL," +
                            "temp DOUBLE PRECISION," +
                            "hr DOUBLE PRECISION," +
                            "rr DOUBLE PRECISION," +
                            "sys DOUBLE PRECISION," +
                            "dia DOUBLE PRECISION," +
                            "ecg DOUBLE PRECISION," +
                            "PRIMARY KEY (patient_id, ts_ms)" +
                            ")"
            );

            // minute_averages
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS minute_averages (" +
                            "patient_id TEXT NOT NULL," +
                            "minute_start_ms BIGINT NOT NULL," +
                            "avg_temp DOUBLE PRECISION," +
                            "avg_hr DOUBLE PRECISION," +
                            "avg_rr DOUBLE PRECISION," +
                            "avg_sys DOUBLE PRECISION," +
                            "avg_dia DOUBLE PRECISION," +
                            "sample_count INT," +
                            "PRIMARY KEY (patient_id, minute_start_ms)" +
                            ")"
            );

            // abnormal_events（如果你有用到就保留；没有也不影响）
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS abnormal_events (" +
                            "patient_id TEXT NOT NULL," +
                            "ts_ms BIGINT NOT NULL," +
                            "level TEXT," +
                            "vital TEXT," +
                            "value DOUBLE PRECISION," +
                            "message TEXT" +
                            ")"
            );
        }
    }
}
