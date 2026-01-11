package rpm.dao;

import rpm.data.AbnormalEvent;
import rpm.db.Db;
import rpm.model.AlertLevel;
import rpm.model.VitalType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class AbnormalEventDao {

    public void insert(String patientId, AbnormalEvent e) throws SQLException {
        String sql =
                "INSERT INTO abnormal_events " +
                        "(patient_id, timestamp_ms, vital_type, level, value, message) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, e.timestampMs());
            ps.setString(3, e.vitalType().name());
            ps.setString(4, e.level().name());
            ps.setDouble(5, e.value());
            ps.setString(6, e.message());
            ps.executeUpdate();
        }
    }

    public List<AbnormalEvent> latest(String patientId, int limit) throws SQLException {
        String sql =
                "SELECT timestamp_ms, vital_type, level, value, message " +
                        "FROM abnormal_events " +
                        "WHERE patient_id = ? " +
                        "ORDER BY timestamp_ms DESC " +
                        "LIMIT ?";

        List<AbnormalEvent> out = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getLong("timestamp_ms");
                    VitalType type = VitalType.valueOf(rs.getString("vital_type"));
                    AlertLevel level = AlertLevel.valueOf(rs.getString("level"));
                    double value = rs.getDouble("value");
                    String msg = rs.getString("message");
                    out.add(new AbnormalEvent(ts, type, level, value, msg));
                }
            }
        }

        return out;
    }
}
