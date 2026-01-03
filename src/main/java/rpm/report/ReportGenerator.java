package rpm.report;

import rpm.alert.AlertEngine;
import rpm.data.MinuteRecord;
import rpm.model.AlertLevel;
import rpm.model.Patient;
import rpm.model.VitalType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.ToDoubleFunction;

public class ReportGenerator {

    private static final List<VitalType> VITALS = Arrays.asList(
            VitalType.BODY_TEMPERATURE,
            VitalType.HEART_RATE,
            VitalType.RESPIRATORY_RATE,
            VitalType.SYSTOLIC_BP,
            VitalType.DIASTOLIC_BP
    );

    public DailyReport generate24hReport(Patient patient,
                                         List<MinuteRecord> records,
                                         AlertEngine alertEngine) {

        long now = System.currentTimeMillis();
        String title = "Daily Report (Last 24 hours) - " + patient.getPatientId() + " - " + patient.getName();

        Map<VitalType, DailyReport.VitalSummary> summaries = new EnumMap<>(VitalType.class);
        List<DailyReport.AbnormalItem> abnormalItems = new ArrayList<>();

        if (records == null) records = Collections.emptyList();

        // mapping extractor (Java 11 compatible)
        Map<VitalType, ToDoubleFunction<MinuteRecord>> extractor = new EnumMap<>(VitalType.class);
        extractor.put(VitalType.BODY_TEMPERATURE, r -> r.getAvgTemp());
        extractor.put(VitalType.HEART_RATE, r -> r.getAvgHR());
        extractor.put(VitalType.RESPIRATORY_RATE, r -> r.getAvgRR());
        extractor.put(VitalType.SYSTOLIC_BP, r -> r.getAvgSys());
        extractor.put(VitalType.DIASTOLIC_BP, r -> r.getAvgDia());

        for (VitalType vt : VITALS) {
            ToDoubleFunction<MinuteRecord> ex = extractor.get(vt);
            if (ex == null) continue;

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0.0;

            int n = 0;
            int warn = 0;
            int urg = 0;

            for (MinuteRecord r : records) {
                double v = ex.applyAsDouble(r);
                if (Double.isNaN(v) || Double.isInfinite(v)) continue;

                n++;
                sum += v;
                min = Math.min(min, v);
                max = Math.max(max, v);

                AlertLevel level = alertEngine.eval(vt, v);
                if (level == AlertLevel.WARNING) warn++;
                if (level == AlertLevel.URGENT) urg++;

                if (level != AlertLevel.NORMAL) {
                    abnormalItems.add(new DailyReport.AbnormalItem(r.getMinuteStartMs(), vt, level, v));
                }
            }

            double avg = (n == 0) ? Double.NaN : (sum / n);
            if (n == 0) {
                min = Double.NaN;
                max = Double.NaN;
            }

            summaries.put(vt, new DailyReport.VitalSummary(min, max, avg, n, warn, urg));
        }

        // abnormal items: sort by time ascending
        abnormalItems.sort(Comparator.comparingLong(DailyReport.AbnormalItem::getMinuteStartMs));

        return new DailyReport(title, now, summaries, abnormalItems);
    }

    public String toText(DailyReport report, Patient patient) {
        StringBuilder sb = new StringBuilder();

        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        sb.append(report.getTitle()).append("\n");
        sb.append("Generated at: ").append(dt.format(Instant.ofEpochMilli(report.getGeneratedAtMs()))).append("\n\n");

        sb.append("Patient details:\n");
        sb.append("  ID: ").append(patient.getPatientId()).append("\n");
        sb.append("  Name: ").append(patient.getName()).append("\n");
        sb.append("  Age: ").append(patient.getAge()).append("\n");
        sb.append("  Ward: ").append(patient.getWard()).append("\n");
        sb.append("  Email: ").append(patient.getEmail()).append("\n");
        sb.append("  Emergency contact: ").append(patient.getEmergencyContact()).append("\n\n");

        sb.append("1) Vital signs value range (minute averages)\n");
        sb.append("--------------------------------------------------\n");

        for (VitalType vt : Arrays.asList(
                VitalType.BODY_TEMPERATURE,
                VitalType.HEART_RATE,
                VitalType.RESPIRATORY_RATE,
                VitalType.SYSTOLIC_BP,
                VitalType.DIASTOLIC_BP
        )) {
            DailyReport.VitalSummary s = report.getSummaries().get(vt);
            if (s == null) continue;

            sb.append(String.format(
                    "%-18s  min=%8.2f  max=%8.2f  avg=%8.2f  minutes=%4d  warning=%3d  urgent=%3d%n",
                    pretty(vt),
                    s.getMin(), s.getMax(), s.getAvg(),
                    s.getTotalMinutes(), s.getWarningMinutes(), s.getUrgentMinutes()
            ));
        }
        sb.append("\n");

        sb.append("2) Abnormal details (minute-level)\n");
        sb.append("--------------------------------------------------\n");
        if (report.getAbnormalItems().isEmpty()) {
            sb.append("No abnormal minutes detected in the available data.\n");
        } else {
            for (DailyReport.AbnormalItem item : report.getAbnormalItems()) {
                sb.append(String.format("%s  %-18s  level=%-7s  value=%.2f%n",
                        dt.format(Instant.ofEpochMilli(item.getMinuteStartMs())),
                        pretty(item.getVitalType()),
                        item.getLevel(),
                        item.getValue()));
            }
        }

        return sb.toString();
    }

    private String pretty(VitalType vt) {
        switch (vt) {
            case BODY_TEMPERATURE:
                return "Body temperature";
            case HEART_RATE:
                return "Heart rate";
            case RESPIRATORY_RATE:
                return "Respiratory rate";
            case SYSTOLIC_BP:
                return "Systolic BP";
            case DIASTOLIC_BP:
                return "Diastolic BP";
            default:
                return vt.name();
        }
    }
}
