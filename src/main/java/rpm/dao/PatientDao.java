package rpm.dao;

import rpm.db.Db;
import rpm.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient DAO.
 *
 * Local-friendly:
 * - If PG* env vars are missing (local run), NO-OP instead of throwing.
 * - On Tsuru (cloud), PG* env vars exist -> normal DB persistence.
 */

public final class PatientDao {

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    public void upsert(Patient p) {
        if (!hasPgEnv()) return;

        String sql =
                "INSERT INTO patients (patient_id, name, age, ward, email, emergency_contact) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (patient_id) DO UPDATE SET " +
                        "name = EXCLUDED.name, " +
                        "age = EXCLUDED.age, " +
                        "ward = EXCLUDED.ward, " +
                        "email = EXCLUDED.email, " +
                        "emergency_contact = EXCLUDED.emergency_contact";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.patientId());
            ps.setString(2, p.name());
            ps.setInt(3, p.age());
            ps.setString(4, p.ward());
            ps.setString(5, p.email());
            ps.setString(6, p.emergencyContact());

            ps.executeUpdate();
        } catch (Exception ignored) {
            // swallow to avoid local console spam
        }
    }

    public java.util.List<rpm.model.Patient> listAll() {
        String sql = "SELECT patient_id, name, age, ward, email, emergency_contact " +
                "FROM patients ORDER BY patient_id ASC";

        java.util.List<rpm.model.Patient> out = new java.util.ArrayList<>();

        try (java.sql.Connection c = rpm.db.Db.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rpm.model.Patient p = new rpm.model.Patient(
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("ward"),
                        rs.getString("email"),
                        rs.getString("emergency_contact")
                );
                out.add(p);
            }

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("PatientDao.listAll failed: " + e.getMessage(), e);
        }

        return out;
    }

}
