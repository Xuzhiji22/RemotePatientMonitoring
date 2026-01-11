package rpm.dao;

import rpm.db.Db;
import rpm.data.MinuteRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class MinuteAverageDao {

    public void upsert(String patientId, MinuteRecord r) throws SQLException {
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

    // 最常用：取最近 N 条 minute record（给 UI/客户端用）
    public List<MinuteRecord> latest(String patientId, int limit) throws SQLException {
        String sql =
                "SELECT minute_start_ms, avg_temp, avg_hr, avg_rr, avg_sys, avg_dia, sample_count " +
                        "FROM minute_averages " +
                        "WHERE patient_id = ? " +
                        "ORDER BY minute_start_ms DESC " +
                        "LIMIT ?";

        List<MinuteRecord> out = new ArrayList<>();
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

    // 可选：按时间范围查（做日报/回放更正规）
    public List<MinuteRecord> range(String patientId, long fromMs, long toMs) throws SQLException {
        String sql =
                "SELECT minute_start_ms, avg_temp, avg_hr, avg_rr, avg_sys, avg_dia, sample_count " +
                        "FROM minute_averages " +
                        "WHERE patient_id = ? AND minute_start_ms >= ? AND minute_start_ms <= ? " +
                        "ORDER BY minute_start_ms ASC";

        List<MinuteRecord> out = new ArrayList<>();
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
