package rpm.dao;

import rpm.db.Db;
import rpm.model.VitalSample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Vital samples DAO.
 *
 * Local-friendly:
 * - If PG* env vars are missing (local run), NO-OP / return empty instead of throwing.
 * - On Tsuru (cloud), PG* env vars exist -> normal DB persistence.
 */
public final class VitalSampleDao {

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    public void insert(String patientId, VitalSample sample) {
        if (!hasPgEnv()) return;

        String sql =
                "INSERT INTO vital_samples " +
                        "(patient_id, ts_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, sample.timestampMs());
            ps.setDouble(3, sample.bodyTemp());
            ps.setDouble(4, sample.heartRate());
            ps.setDouble(5, sample.respiratoryRate());
            ps.setDouble(6, sample.systolicBP());
            ps.setDouble(7, sample.diastolicBP());
            ps.setDouble(8, sample.ecgValue());

            ps.executeUpdate();
        } catch (Exception ignored) {
            // swallow to avoid local / console spam
        }
    }

    public List<VitalSample> latest(String patientId, int limit) {
        List<VitalSample> out = new ArrayList<>();
        if (!hasPgEnv()) return out;

        String sql =
                "SELECT ts_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value " +
                        "FROM vital_samples WHERE patient_id = ? " +
                        "ORDER BY ts_ms DESC LIMIT ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getLong("ts_ms");
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
        }

        return out;
    }

    public List<VitalSample> range(String patientId, long fromMs, long toMs, int limit) {
        List<VitalSample> out = new ArrayList<>();
        if (!hasPgEnv()) return out;

        String sql =
                "SELECT ts_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value " +
                        "FROM vital_samples WHERE patient_id = ? AND ts_ms BETWEEN ? AND ? " +
                        "ORDER BY ts_ms DESC LIMIT ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, fromMs);
            ps.setLong(3, toMs);
            ps.setInt(4, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getLong("ts_ms");
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
        }

        return out;
    }
}
