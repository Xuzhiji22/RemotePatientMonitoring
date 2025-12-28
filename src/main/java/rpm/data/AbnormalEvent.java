package rpm.data;

import rpm.model.AlertLevel;
import rpm.model.VitalType;

public record AbnormalEvent(
        long timestampMs,
        VitalType vitalType,
        AlertLevel level,
        double value,
        String message
) {}
