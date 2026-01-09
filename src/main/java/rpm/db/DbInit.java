package rpm.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DbInit {
    private DbInit() {}

    public static void init() {
        try (Connection c = Db.getConnection();
             Statement st = c.createStatement()) {

            // 1) patients：匹配 rpm.model.Patient
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

            // 2) vital_samples：匹配 rpm.model.VitalSample（带 patient_id 外键 + timestamp）
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

            // 3) index：加速按 patient + 时间查询
            String createIndex =
                    "CREATE INDEX IF NOT EXISTS idx_vitals_patient_ts " +
                            "ON vital_samples(patient_id, ts_ms DESC)";
            st.execute(createIndex);

        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }
}
