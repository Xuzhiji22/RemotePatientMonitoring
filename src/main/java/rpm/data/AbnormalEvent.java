package rpm.data;

import rpm.model.AlertLevel;
import rpm.model.VitalType;

import java.util.Objects;

public final class AbnormalEvent {

    private final long timestampMs;
    private final VitalType vitalType;
    private final AlertLevel level;
    private final double value;
    private final String message;

    public AbnormalEvent(
            long timestampMs,
            VitalType vitalType,
            AlertLevel level,
            double value,
            String message
    ) {
        this.timestampMs = timestampMs;
        this.vitalType = vitalType;
        this.level = level;
        this.value = value;
        this.message = message;
    }

    public long timestampMs() {
        return timestampMs;
    }

    public VitalType vitalType() {
        return vitalType;
    }

    public AlertLevel level() {
        return level;
    }

    public double value() {
        return value;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbnormalEvent)) return false;

        AbnormalEvent other = (AbnormalEvent) o;
        return timestampMs == other.timestampMs()
                && Double.compare(other.value(), value) == 0
                && vitalType == other.vitalType()
                && level == other.level()
                && Objects.equals(message, other.message());
    }


    @Override
    public int hashCode() {
        return Objects.hash(timestampMs, vitalType, level, value, message);
    }

    @Override
    public String toString() {
        return "AbnormalEvent[" +
                "timestampMs=" + timestampMs +
                ", vitalType=" + vitalType +
                ", level=" + level +
                ", value=" + value +
                ", message=" + message +
                "]";
    }
}
