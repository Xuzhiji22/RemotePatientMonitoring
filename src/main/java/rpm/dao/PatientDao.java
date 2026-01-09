package rpm.dao;

import rpm.db.Db;
import rpm.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PatientDao {

    public void upsert(Patient p) throws SQLException {
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
        }
    }

    public Optional<Patient> findById(String patientId) throws SQLException {
        String sql = "SELECT patient_id, name, age, ward, email, emergency_contact FROM patients WHERE patient_id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                return Optional.of(new Patient(
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("ward"),
                        rs.getString("email"),
                        rs.getString("emergency_contact")
                ));
            }
        }
    }

    public List<Patient> listAll() throws SQLException {
        String sql = "SELECT patient_id, name, age, ward, email, emergency_contact FROM patients ORDER BY patient_id";
        List<Patient> out = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new Patient(
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("ward"),
                        rs.getString("email"),
                        rs.getString("emergency_contact")
                ));
            }
        }
        return out;
    }
}
