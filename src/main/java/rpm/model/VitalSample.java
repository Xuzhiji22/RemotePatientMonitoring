package rpm.model;

import java.util.Objects;

public final class VitalSample {

    private final long timestampMs;
    private final double bodyTemp;
    private final double heartRate;
    private final double respiratoryRate;
    private final double systolicBP;
    private final double diastolicBP;
    private final double ecgValue;

    public VitalSample(
            long timestampMs,
            double bodyTemp,
            double heartRate,
            double respiratoryRate,
            double systolicBP,
            double diastolicBP,
            double ecgValue
    ) {
        this.timestampMs = timestampMs;
        this.bodyTemp = bodyTemp;
        this.heartRate = heartRate;
        this.respiratoryRate = respiratoryRate;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.ecgValue = ecgValue;
    }

    public long timestampMs() {
        return timestampMs;
    }

    public double bodyTemp() {
        return bodyTemp;
    }

    public double heartRate() {
        return heartRate;
    }

    public double respiratoryRate() {
        return respiratoryRate;
    }

    public double systolicBP() {
        return systolicBP;
    }

    public double diastolicBP() {
        return diastolicBP;
    }

    public double ecgValue() {
        return ecgValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VitalSample)) return false;

        VitalSample other = (VitalSample) o;
        return timestampMs == other.timestampMs()
                && Double.compare(other.bodyTemp(), bodyTemp) == 0
                && Double.compare(other.heartRate(), heartRate) == 0
                && Double.compare(other.respiratoryRate(), respiratoryRate) == 0
                && Double.compare(other.systolicBP(), systolicBP) == 0
                && Double.compare(other.diastolicBP(), diastolicBP) == 0
                && Double.compare(other.ecgValue(), ecgValue) == 0;
    }


    @Override
    public int hashCode() {
        return Objects.hash(
                timestampMs,
                bodyTemp,
                heartRate,
                respiratoryRate,
                systolicBP,
                diastolicBP,
                ecgValue
        );
    }

    @Override
    public String toString() {
        return "VitalSample[" +
                "timestampMs=" + timestampMs +
                ", bodyTemp=" + bodyTemp +
                ", heartRate=" + heartRate +
                ", respiratoryRate=" + respiratoryRate +
                ", systolicBP=" + systolicBP +
                ", diastolicBP=" + diastolicBP +
                ", ecgValue=" + ecgValue +
                "]";
    }
}
