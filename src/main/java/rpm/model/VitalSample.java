package rpm.model;

public final class VitalSample {

    private final long timestampMs;
    private final double bodyTemp;
    private final double heartRate;
    private final double respiratoryRate;
    private final double systolicBP;
    private final double diastolicBP;
    private final double ecgValue;

    public VitalSample(long timestampMs,
                       double bodyTemp,
                       double heartRate,
                       double respiratoryRate,
                       double systolicBP,
                       double diastolicBP,
                       double ecgValue) {
        this.timestampMs = timestampMs;
        this.bodyTemp = bodyTemp;
        this.heartRate = heartRate;
        this.respiratoryRate = respiratoryRate;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.ecgValue = ecgValue;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public double getBodyTemp() {
        return bodyTemp;
    }

    public double getHeartRate() {
        return heartRate;
    }

    public double getRespiratoryRate() {
        return respiratoryRate;
    }

    public double getSystolicBP() {
        return systolicBP;
    }

    public double getDiastolicBP() {
        return diastolicBP;
    }

    public double getEcgValue() {
        return ecgValue;
    }
}
