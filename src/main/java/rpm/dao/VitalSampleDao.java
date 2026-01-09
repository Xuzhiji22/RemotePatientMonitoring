package rpm.dao;

import rpm.db.Db;
import rpm.model.VitalSample;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class VitalSampleDao {

    public void insert(String patientId, VitalSample s) throws SQLException {
        String sql =
                "INSERT INTO vital_samples " +
                        "(patient_id, ts_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value) " +
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
        }
    }

    public List<VitalSample> latest(String patientId, int limit) throws SQLException {
        String sql =
                "SELECT ts_ms, body_temp, heart_rate, respiratory_rate, systolic_bp, diastolic_bp, ecg_value " +
                        "FROM vital_samples " +
                        "WHERE patient_id = ? " +
                        "ORDER BY ts_ms DESC " +
                        "LIMIT ?";


        List<VitalSample> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new VitalSample(
                            rs.getLong("ts_ms"),
                            rs.getDouble("body_temp"),
                            rs.getDouble("heart_rate"),
                            rs.getDouble("respiratory_rate"),
                            rs.getDouble("systolic_bp"),
                            rs.getDouble("diastolic_bp"),
                            rs.getDouble("ecg_value")
                    ));
                }
            }
        }
        return out;
    }
}
