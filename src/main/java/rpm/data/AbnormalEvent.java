package rpm.data;

import rpm.model.AlertLevel;
import rpm.model.VitalType;

public final class AbnormalEvent {

    private final long timestampMs;
    private final VitalType vitalType;
    private final AlertLevel level;
    private final double value;
    private final String message;

    public AbnormalEvent(long timestampMs,
                         VitalType vitalType,
                         AlertLevel level,
                         double value,
                         String message) {
        this.timestampMs = timestampMs;
        this.vitalType = vitalType;
        this.level = level;
        this.value = value;
        this.message = message;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public VitalType getVitalType() {
        return vitalType;
    }

    public AlertLevel getLevel() {
        return level;
    }

    public double getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }
}
