package rpm.notify;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.Patient;
import rpm.model.VitalType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class DailyDigestNotifier {

    private final EmailService email;
    private final AlertEngine alertEngine;
    private final String doctorEmail;

    // key: patientId -> stats
    private final Map<String, PatientDailyStats> statsByPatient = new HashMap<>();

    // to ensure "once per day"
    private LocalDate lastSentDate = null;

    public DailyDigestNotifier(EmailService email, AlertEngine alertEngine, String doctorEmail) {
        this.email = email;
        this.alertEngine = alertEngine;
        this.doctorEmail = doctorEmail;
    }

    /** Use "minute average" to identify anomalies */
    public synchronized void onMinuteAverage(Patient patient, long bucketStartMs,
                                             double avgTemp, double avgHR, double avgRR, double avgSys, double avgDia) {

        PatientDailyStats st = statsByPatient.computeIfAbsent(patient.patientId(),
                id -> new PatientDailyStats(patient.patientId(), patient.name(), patient.ward()));

        st.totalMinutes++;

        //Evaluate the alarm level for each minute average.
        addIfAbnormal(st, VitalType.BODY_TEMPERATURE, avgTemp);
        addIfAbnormal(st, VitalType.HEART_RATE, avgHR);
        addIfAbnormal(st, VitalType.RESPIRATORY_RATE, avgRR);
        addIfAbnormal(st, VitalType.SYSTOLIC_BP, avgSys);
        addIfAbnormal(st, VitalType.DIASTOLIC_BP, avgDia);
    }

    /** Called once a day at a fixed time: Send + Clear */
    public synchronized void sendDailyIfDue(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        if (lastSentDate != null && lastSentDate.equals(today)) return; // already sent today

        // If there's no data yet today, you don't have to post; can also post every day.
        if (statsByPatient.isEmpty()) {
            lastSentDate = today; // 防止重复尝试
            return;
        }

        String subject = "[RPM Daily Summary] " + today;
        String body = buildBody(today);

        email.send(doctorEmail, subject, body);

        // reset for next day
        statsByPatient.clear();
        lastSentDate = today;
    }

    private void addIfAbnormal(PatientDailyStats st, VitalType type, double value) {
        AlertLevel lv = alertEngine.eval(type, value);
        if (lv == AlertLevel.NORMAL) return;

        st.abnormalMinutes++; // 这一分钟里至少有一个 abnormal（粗略统计：每 abnormal 计一次）
        // 如果你想“每分钟最多算一次 abnormalMinutes”，可以做去重；见下方备注

        if (lv == AlertLevel.WARNING) st.warningCount++;
        if (lv == AlertLevel.URGENT) st.urgentCount++;

        st.vitalCounts.get(type).total++;
        if (lv == AlertLevel.WARNING) st.vitalCounts.get(type).warning++;
        if (lv == AlertLevel.URGENT) st.vitalCounts.get(type).urgent++;
    }

    private String buildBody(LocalDate today) {
        StringBuilder sb = new StringBuilder();
        sb.append("RPM Daily Digest\n");
        sb.append("Date: ").append(today).append("\n");
        sb.append("Patients covered: ").append(statsByPatient.size()).append("\n");
        sb.append("========================================\n\n");

        for (PatientDailyStats st : statsByPatient.values()) {
            sb.append("Patient: ").append(st.patientId).append(" (").append(st.name).append(")\n");
            sb.append("Ward: ").append(st.ward).append("\n");
            sb.append("Minutes observed: ").append(st.totalMinutes).append("\n");
            sb.append("Abnormal hits (minute-level): ").append(st.abnormalMinutes).append("\n");
            sb.append("WARNING count: ").append(st.warningCount).append("\n");
            sb.append("URGENT count: ").append(st.urgentCount).append("\n");

            sb.append("By vital:\n");
            for (VitalType t : VitalType.values()) {
                VitalCounter vc = st.vitalCounts.get(t);
                if (vc == null) continue;
                if (vc.total == 0) continue;
                sb.append("  - ").append(pretty(t))
                        .append(": total=").append(vc.total)
                        .append(", WARNING=").append(vc.warning)
                        .append(", URGENT=").append(vc.urgent)
                        .append("\n");
            }

            sb.append("----------------------------------------\n");
        }

        sb.append("\nNotes:\n");
        sb.append("- This digest is computed from per-minute averages (more stable than per-sample alerts).\n");
        return sb.toString();
    }

    private String pretty(VitalType t) {
        switch (t) {
            case BODY_TEMPERATURE: return "Body Temp";
            case HEART_RATE: return "Heart Rate";
            case RESPIRATORY_RATE: return "Resp Rate";
            case SYSTOLIC_BP: return "Systolic BP";
            case DIASTOLIC_BP: return "Diastolic BP";
            default: return t.toString();
        }
    }

    // ---------- inner stats ----------
    private static class PatientDailyStats {
        final String patientId;
        final String name;
        final String ward;

        int totalMinutes = 0;
        int abnormalMinutes = 0;
        int warningCount = 0;
        int urgentCount = 0;

        final Map<VitalType, VitalCounter> vitalCounts = new EnumMap<>(VitalType.class);

        PatientDailyStats(String patientId, String name, String ward) {
            this.patientId = patientId;
            this.name = name;
            this.ward = ward;

            // init only the vitals you care about (or all)
            vitalCounts.put(VitalType.BODY_TEMPERATURE, new VitalCounter());
            vitalCounts.put(VitalType.HEART_RATE, new VitalCounter());
            vitalCounts.put(VitalType.RESPIRATORY_RATE, new VitalCounter());
            vitalCounts.put(VitalType.SYSTOLIC_BP, new VitalCounter());
            vitalCounts.put(VitalType.DIASTOLIC_BP, new VitalCounter());
        }
    }

    private static class VitalCounter {
        int total = 0;
        int warning = 0;
        int urgent = 0;
    }
}
