package rpm.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Threshold logic.
 * Ensures that values are correctly categorized as NORMAL, WARNING, or URGENT.
 */
class ThresholdTest {

    @Test
    void testEvaluateLogic() {
        // Set up a threshold:
        // Normal range: 36.0 - 37.5
        // Safe range (incl. Warning): 35.0 - 39.0
        // Outside 35.0 - 39.0 is URGENT
        Threshold t = new Threshold(36.0, 37.5, 35.0, 39.0);

        // 1. Test NORMAL range
        assertEquals(AlertLevel.NORMAL, t.evaluate(36.5), "36.5 should be NORMAL");
        assertEquals(AlertLevel.NORMAL, t.evaluate(37.0), "37.0 should be NORMAL");

        // 2. Test WARNING range (Values between Normal and Urgent limits)
        assertEquals(AlertLevel.WARNING, t.evaluate(37.8), "37.8 should be WARNING (High)");
        assertEquals(AlertLevel.WARNING, t.evaluate(35.5), "35.5 should be WARNING (Low)");

        // 3. Test URGENT range (Values outside the safe limits)
        assertEquals(AlertLevel.URGENT, t.evaluate(40.0), "40.0 should be URGENT (High Fever)");
        assertEquals(AlertLevel.URGENT, t.evaluate(34.0), "34.0 should be URGENT (Hypothermia)");

        // 4. Test Boundary values
        // Based on logic: if (v < urgentLow), so equality falls through to next check
        assertEquals(AlertLevel.WARNING, t.evaluate(35.0), "Boundary check: 35.0 should be WARNING");
    }
}