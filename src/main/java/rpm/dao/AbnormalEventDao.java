package rpm.dao;

import rpm.data.AbnormalEvent;
import rpm.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class AbnormalEventDao {

    public void insert(String patientId, AbnormalEvent e) throws SQLException {
        String sql =
                "INSERT INTO abnormal_events " +
                        "(patient_id, ts_ms, vital_type, level, value, message) " +
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
}
