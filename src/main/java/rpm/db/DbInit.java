package rpm.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DbInit {
    private DbInit() {}

    public static void init() {
        try (Connection c = Db.getConnection();
             Statement st = c.createStatement()) {

            // 1) patients
            String createPatients =
                    "CREATE TABLE IF NOT EXISTS patients (" +
                            "patient_id VARCHAR(64) PRIMARY KEY, " +
                            "name VARCHAR(256) NOT NULL, " +
                            "age INT NOT NULL, " +
                            "ward VARCHAR(128), " +
                            "email VARCHAR(256), " +
                            "emergency_contact VARCHAR(256)" +
                            ")";
            st.execute(createPatients);

            // 2) vital_samples
            String createVitals =
                    "CREATE TABLE IF NOT EXISTS vital_samples (" +
                            "id BIGSERIAL PRIMARY KEY, " +
                            "patient_id VARCHAR(64) NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE, " +
                            "ts_ms BIGINT NOT NULL, " +
                            "body_temp DOUBLE PRECISION, " +
                            "heart_rate DOUBLE PRECISION, " +
                            "respiratory_rate DOUBLE PRECISION, " +
                            "systolic_bp DOUBLE PRECISION, " +
                            "diastolic_bp DOUBLE PRECISION, " +
                            "ecg_value DOUBLE PRECISION" +
                            ")";
            st.execute(createVitals);

            // 3) index：
            String createIndex =
                    "CREATE INDEX IF NOT EXISTS idx_vitals_patient_ts " +
                            "ON vital_samples(patient_id, ts_ms DESC)";
            st.execute(createIndex);

            // minute_averages: one row per patient per minute
            String sqlMinute =
                    "CREATE TABLE IF NOT EXISTS minute_averages (" +
                            "patient_id VARCHAR(64) NOT NULL," +
                            "minute_start_ms BIGINT NOT NULL," +
                            "avg_temp DOUBLE PRECISION NOT NULL," +
                            "avg_hr DOUBLE PRECISION NOT NULL," +
                            "avg_rr DOUBLE PRECISION NOT NULL," +
                            "avg_sys DOUBLE PRECISION NOT NULL," +
                            "avg_dia DOUBLE PRECISION NOT NULL," +
                            "sample_count INT NOT NULL," +
                            "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                            "PRIMARY KEY (patient_id, minute_start_ms)" +
                            ")";

            st.execute(sqlMinute);
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }
}
