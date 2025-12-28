package rpm.model;

public record VitalSample(
        long timestampMs,
        double bodyTemp,
        double heartRate,
        double respiratoryRate,
        double systolicBP,
        double diastolicBP,
        double ecgValue
) {}
