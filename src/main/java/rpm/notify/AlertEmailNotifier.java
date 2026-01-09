package rpm.notify;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.Patient;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class AlertEmailNotifier {

    private final EmailService email;
    private final AlertEngine alertEngine;
    private final String doctorEmail;

    // same patient+vital+level minimum interval (ms)
    private final long cooldownMs;
    private final Map<String, Long> lastSentAt = new HashMap<>();

    private static final EnumSet<VitalType> VITALS = EnumSet.of(
            VitalType.BODY_TEMPERATURE,
            VitalType.HEART_RATE,
            VitalType.RESPIRATORY_RATE,
            VitalType.SYSTOLIC_BP,
            VitalType.DIASTOLIC_BP
    );

    public AlertEmailNotifier(EmailService email, AlertEngine alertEngine, String doctorEmail, long cooldownMs) {
        this.email = email;
        this.alertEngine = alertEngine;
        this.doctorEmail = doctorEmail;
        this.cooldownMs = cooldownMs;
    }

    /** Real-time sample trigger: recommend URGENT only (or include WARNING if sendWarningsToo=true) */
    public void onRealtimeSample(Patient patient, VitalSample s, boolean sendWarningsToo) {
        long now = System.currentTimeMillis();

        for (VitalType t : VITALS) {
            double v = valueOf(t, s);
            AlertLevel lv = alertEngine.eval(t, v);

            if (lv == AlertLevel.NORMAL) continue;
            if (!sendWarningsToo && lv != AlertLevel.URGENT) continue;

            String key = patient.patientId() + "|" + t + "|" + lv;
            if (!shouldSend(now, key)) continue;

            String subject = "[RPM " + lv + "] " + patient.patientId() + " " + pretty(t);
            String body =
                    "Patient: " + patient.patientId() + " (" + patient.name() + ")\n" +
                            "Ward: " + patient.ward() + "\n" +
                            "Vital: " + pretty(t) + "\n" +
                            "Value: " + format(t, v) + "\n" +
                            "Level: " + lv + "\n";

            email.send(doctorEmail, subject, body);
        }
    }

    /** Minute-average trigger: send abnormal summary if any abnormal vitals exist */
    public void onMinuteAverage(Patient patient, long bucketStartMs,
                                double avgTemp, double avgHR, double avgRR, double avgSys, double avgDia) {

        StringBuilder sb = new StringBuilder();
        int abnormalCount = 0;

        abnormalCount += appendIfAbn(sb, patient, bucketStartMs, VitalType.BODY_TEMPERATURE, avgTemp);
        abnormalCount += appendIfAbn(sb, patient, bucketStartMs, VitalType.HEART_RATE, avgHR);
        abnormalCount += appendIfAbn(sb, patient, bucketStartMs, VitalType.RESPIRATORY_RATE, avgRR);
        abnormalCount += appendIfAbn(sb, patient, bucketStartMs, VitalType.SYSTOLIC_BP, avgSys);
        abnormalCount += appendIfAbn(sb, patient, bucketStartMs, VitalType.DIASTOLIC_BP, avgDia);

        if (abnormalCount == 0) return;

        String subject = "[RPM Abnormal Summary] " + patient.patientId() + " (" + abnormalCount + " abnormal)";
        email.send(doctorEmail, subject, sb.toString());
    }

    public void sendReportToDoctor(Patient patient, String reportText) {
        String subject = "[RPM Report] " + patient.patientId() + " " + patient.name();
        email.send(doctorEmail, subject, reportText);
    }

    private int appendIfAbn(StringBuilder sb, Patient p, long t0, VitalType type, double value) {
        AlertLevel lv = alertEngine.eval(type, value);
        if (lv == AlertLevel.NORMAL) return 0;

        sb.append("Patient: ").append(p.patientId()).append(" (").append(p.name()).append(")\n");
        sb.append("BucketStart(ms): ").append(t0).append("\n");
        sb.append("Vital: ").append(pretty(type)).append("\n");
        sb.append("Value(avg): ").append(format(type, value)).append("\n");
        sb.append("Level: ").append(lv).append("\n");
        sb.append("----------------------------------------\n");
        return 1;
    }

    private boolean shouldSend(long now, String key) {
        Long last = lastSentAt.get(key);
        if (last != null && now - last < cooldownMs) return false;
        lastSentAt.put(key, now);
        return true;
    }

    private double valueOf(VitalType t, VitalSample s) {
        switch (t) {
            case BODY_TEMPERATURE:
                return s.bodyTemp();
            case HEART_RATE:
                return s.heartRate();
            case RESPIRATORY_RATE:
                return s.respiratoryRate();
            case SYSTOLIC_BP:
                return s.systolicBP();
            case DIASTOLIC_BP:
                return s.diastolicBP();
            default:
                return Double.NaN;
        }
    }

    private String pretty(VitalType t) {
        switch (t) {
            case BODY_TEMPERATURE:
                return "Body Temp";
            case HEART_RATE:
                return "Heart Rate";
            case RESPIRATORY_RATE:
                return "Resp Rate";
            case SYSTOLIC_BP:
                return "Systolic BP";
            case DIASTOLIC_BP:
                return "Diastolic BP";
            default:
                return t.toString();
        }
    }

    private String format(VitalType t, double v) {
        switch (t) {
            case BODY_TEMPERATURE:
                return String.format("%.2f °C", v);
            case HEART_RATE:
                return String.format("%.0f bpm", v);
            case RESPIRATORY_RATE:
                return String.format("%.0f rpm", v);
            case SYSTOLIC_BP:
            case DIASTOLIC_BP:
                return String.format("%.0f mmHg", v);
            default:
                return String.format("%.2f", v);
        }
    }
}
