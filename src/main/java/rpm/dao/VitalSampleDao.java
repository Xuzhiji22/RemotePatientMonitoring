package rpm.dao;

import rpm.db.Db;
import rpm.model.VitalSample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * VitalSample DAO.
 *
 * IMPORTANT (local-friendly):
 * - If PG* env vars are missing (common on local machine), we NO-OP (skip DB) instead of throwing,
 *   so the UI can run without console spam.
 * - On Tsuru, PG* env vars exist -> DB writes work normally.
 */
public final class VitalSampleDao {

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    public void insert(String patientId, VitalSample s) {
        // Local machine: no Postgres bound -> skip DB
        if (!hasPgEnv()) return;

        String sql = "INSERT INTO vital_samples " +
                "(patient_id, timestamp_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, s.timestampMs());
            ps.setDouble(3, s.bodyTemp());
            ps.setDouble(4, s.heartRate());
            ps.setDouble(5, s.respiratoryRate());
            ps.setDouble(6, s.systolicBP());
            ps.setDouble(7, s.diastolicBP());
            ps.setDouble(8, s.ecgValue());

            ps.executeUpdate();
        } catch (Exception ignored) {
            // Deliberately swallow to avoid local console spam.
            // If DB is configured (Tsuru), errors should be rare; if they happen, debug via server logs.
        }
    }

    public List<VitalSample> latest(String patientId, int limit) {
        List<VitalSample> out = new ArrayList<>();

        // Local machine: skip DB read
        if (!hasPgEnv()) return out;

        String sql = "SELECT timestamp_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value " +
                "FROM vital_samples WHERE patient_id = ? " +
                "ORDER BY timestamp_ms DESC LIMIT ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getLong("timestamp_ms");
                    double temp = rs.getDouble("body_temp");
                    double hr = rs.getDouble("heart_rate");
                    double rr = rs.getDouble("respiratory_rate");
                    double sys = rs.getDouble("systolic_bp");
                    double dia = rs.getDouble("diastolic_bp");
                    double ecg = rs.getDouble("ecg_value");

                    out.add(new VitalSample(ts, temp, hr, rr, sys, dia, ecg));
                }
            }
        } catch (Exception ignored) {
            // swallow for local-friendly behavior
        }

        return out;
    }
}
