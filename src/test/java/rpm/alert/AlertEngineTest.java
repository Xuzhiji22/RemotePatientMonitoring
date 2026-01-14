package rpm.alert;

import org.junit.jupiter.api.Test;
import rpm.model.AlertLevel;
import rpm.model.VitalType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AlertEngine.
 * Verifies that the default thresholds for vital signs are correctly configured.
 */
class AlertEngineTest {

    @Test
    void testHeartRateUrgent() {
        AlertEngine engine = new AlertEngine();

        // Input a very high heart rate (150 bpm)
        // Default Urgent Threshold is > 140
        AlertLevel result = engine.eval(VitalType.HEART_RATE, 150);

        assertEquals(AlertLevel.URGENT, result, "Heart Rate of 150 should be URGENT");
    }

    @Test
    void testHeartRateNormal() {
        AlertEngine engine = new AlertEngine();

        // Input a normal heart rate (75 bpm)
        // Default Normal Range is 50-100
        AlertLevel result = engine.eval(VitalType.HEART_RATE, 75);

        assertEquals(AlertLevel.NORMAL, result, "Heart Rate of 75 should be NORMAL");
    }

    @Test
    void testBodyTemperatureWarning() {
        AlertEngine engine = new AlertEngine();

        // Input a slight fever (38.0 C)
        // Default Warning Range is 37.5 - 39.0
        AlertLevel result = engine.eval(VitalType.BODY_TEMPERATURE, 38.0);

        assertEquals(AlertLevel.WARNING, result, "Temperature of 38.0 should be WARNING");
    }
}