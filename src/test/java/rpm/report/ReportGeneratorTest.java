package rpm.report;

import org.junit.jupiter.api.Test;
import rpm.alert.AlertEngine;
import rpm.data.MinuteRecord;
import rpm.model.Patient;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportGenerator.
 * Verifies that the generated text report contains critical information.
 */
class ReportGeneratorTest {

    @Test
    void testReportContentGeneration() {
        // 1. Setup
        Patient patient = new Patient("P1", "Test User", 30, "Ward A", "patient1@example.com", "999-000-0001");
        AlertEngine engine = new AlertEngine();
        long now = System.currentTimeMillis();

        // Create a record with high fever (39.5 C) which should trigger URGENT alert
        MinuteRecord feverRec = new MinuteRecord(
                now, 39.5, 100.0, 20.0, 130.0, 85.0, 12
        );

        // 2. Action: Generate the report text
        ReportGenerator generator = new ReportGenerator();
        DailyReport report = generator.generate24hReport(patient, List.of(feverRec), engine);
        String reportText = generator.toText(report, patient);

        // 3. Assertion: Verify content
        assertTrue(reportText.contains("P1"), "Report should contain Patient ID");
        assertTrue(reportText.contains("Body temperature"), "Should include Body Temp section");

        // Check for Alert Detection (39.5 C is URGENT)
        assertTrue(reportText.contains("URGENT"), "Report should flag high fever as URGENT");
        assertTrue(reportText.contains("39.5"), "Report should display the abnormal value");
    }

    @Test
    void testEmptyData() {
        Patient patient = new Patient("P2", "Empty User", 30, "Ward B", "patient2@example.com", "999-000-0002");
        AlertEngine engine = new AlertEngine();

        ReportGenerator generator = new ReportGenerator();
        DailyReport report = generator.generate24hReport(patient, Collections.emptyList(), engine);
        String text = generator.toText(report, patient);

        assertNotNull(text);
        assertTrue(text.contains("No abnormal minutes detected"), "Should handle empty history");
    }
}