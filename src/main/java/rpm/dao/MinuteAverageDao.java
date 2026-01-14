package rpm.dao;

import rpm.db.Db;
import rpm.data.MinuteRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class MinuteAverageDao {

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    public void upsert(String patientId, MinuteRecord r) throws SQLException {
        if (!hasPgEnv()) return;

        String sql =
                "INSERT INTO minute_averages " +
                        "(patient_id, minute_start_ms, avg_temp, avg_hr, avg_rr, avg_sys, avg_dia, sample_count) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (patient_id, minute_start_ms) DO UPDATE SET " +
                        "avg_temp = EXCLUDED.avg_temp, " +
                        "avg_hr = EXCLUDED.avg_hr, " +
                        "avg_rr = EXCLUDED.avg_rr, " +
                        "avg_sys = EXCLUDED.avg_sys, " +
                        "avg_dia = EXCLUDED.avg_dia, " +
                        "sample_count = EXCLUDED.sample_count";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, r.minuteStartMs());
            ps.setDouble(3, r.avgTemp());
            ps.setDouble(4, r.avgHR());
            ps.setDouble(5, r.avgRR());
            ps.setDouble(6, r.avgSys());
            ps.setDouble(7, r.avgDia());
            ps.setInt(8, r.sampleCount());
            ps.executeUpdate();
        }
    }

    public List<MinuteRecord> latest(String patientId, int limit) throws SQLException {
        List<MinuteRecord> out = new ArrayList<>();
        if (!hasPgEnv()) return out;

        String sql =
                "SELECT minute_start_ms, avg_temp, avg_hr, avg_rr, avg_sys, avg_dia, sample_count " +
                        "FROM minute_averages " +
                        "WHERE patient_id = ? " +
                        "ORDER BY minute_start_ms DESC " +
                        "LIMIT ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new MinuteRecord(
                            rs.getLong("minute_start_ms"),
                            rs.getDouble("avg_temp"),
                            rs.getDouble("avg_hr"),
                            rs.getDouble("avg_rr"),
                            rs.getDouble("avg_sys"),
                            rs.getDouble("avg_dia"),
                            rs.getInt("sample_count")
                    ));
                }
            }
        }
        return out;
    }

    public List<MinuteRecord> range(String patientId, long fromMs, long toMs) throws SQLException {
        List<MinuteRecord> out = new ArrayList<>();
        if (!hasPgEnv()) return out;

        String sql =
                "SELECT minute_start_ms, avg_temp, avg_hr, avg_rr, avg_sys, avg_dia, sample_count " +
                        "FROM minute_averages " +
                        "WHERE patient_id = ? AND minute_start_ms >= ? AND minute_start_ms <= ? " +
                        "ORDER BY minute_start_ms ASC";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, fromMs);
            ps.setLong(3, toMs);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new MinuteRecord(
                            rs.getLong("minute_start_ms"),
                            rs.getDouble("avg_temp"),
                            rs.getDouble("avg_hr"),
                            rs.getDouble("avg_rr"),
                            rs.getDouble("avg_sys"),
                            rs.getDouble("avg_dia"),
                            rs.getInt("sample_count")
                    ));
                }
            }
        }
        return out;
    }
}
