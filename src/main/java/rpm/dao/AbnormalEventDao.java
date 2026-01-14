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

        // Works even if the table has NO unique/primary key constraint.
        // Avoids Postgres error: "no unique or exclusion constraint matching the ON CONFLICT specification"
        String sql =
                "INSERT INTO abnormal_events " +
                        "(patient_id, timestamp_ms, vital_type, level, value, message) " +
                        "SELECT ?, ?, ?, ?, ?, ? " +
                        "WHERE NOT EXISTS (" +
                        "  SELECT 1 FROM abnormal_events " +
                        "  WHERE patient_id = ? AND timestamp_ms = ? AND vital_type = ?" +
                        ")";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setLong(2, e.timestampMs());
            ps.setString(3, e.vitalType().name());
            ps.setString(4, e.level().name());
            ps.setDouble(5, e.value());
            ps.setString(6, e.message());

            // NOT EXISTS params
            ps.setString(7, patientId);
            ps.setLong(8, e.timestampMs());
            ps.setString(9, e.vitalType().name());

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
                    out.add(new AbnormalEvent(
                            rs.getLong("timestamp_ms"),
                            VitalType.valueOf(rs.getString("vital_type")),
                            AlertLevel.valueOf(rs.getString("level")),
                            rs.getDouble("value"),
                            rs.getString("message")
                    ));
                }
            }
        }
        return out;
    }
}
