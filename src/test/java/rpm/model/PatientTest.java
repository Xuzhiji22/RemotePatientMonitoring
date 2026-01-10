package rpm.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Patient model.
 * Verifies data integrity (Constructor and Getters only).
 */
class PatientTest {

    @Test
    void testPatientCreation() {
        // Create a patient with dummy data
        Patient p = new Patient("P1", "John Doe", 30, "Ward A", "john@test.com", "999");

        // Verify that getters return exactly what was passed in constructor
        assertEquals("P1", p.patientId());
        assertEquals("John Doe", p.name());
        assertEquals(30, p.age());
        assertEquals("Ward A", p.ward());
        assertEquals("john@test.com", p.email());
        assertEquals("999", p.emergencyContact());
    }

    @Test
    void testEquality() {
        // Verify that two patients with same ID and data are considered 'equal'
        // This is important for Lists to work correctly
        Patient p1 = new Patient("P1", "John", 30, "A", "e", "c");
        Patient p2 = new Patient("P1", "John", 30, "A", "e", "c");

        assertEquals(p1, p2, "Two patient objects with same data should be equal");
    }
}