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

    private static final List<VitalType> VITALS = List.of(
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
        String title = "Daily Report (Last 24 hours) - " + patient.patientId() + " - " + patient.name();

        Map<VitalType, DailyReport.VitalSummary> summaries = new EnumMap<>(VitalType.class);
        List<DailyReport.AbnormalItem> abnormalItems = new ArrayList<>();

        if (records == null) records = List.of();

        // mapping extractor
        Map<VitalType, ToDoubleFunction<MinuteRecord>> extractor = Map.of(
                VitalType.BODY_TEMPERATURE, MinuteRecord::avgTemp,
                VitalType.HEART_RATE, MinuteRecord::avgHR,
                VitalType.RESPIRATORY_RATE, MinuteRecord::avgRR,
                VitalType.SYSTOLIC_BP, MinuteRecord::avgSys,
                VitalType.DIASTOLIC_BP, MinuteRecord::avgDia
        );

        for (VitalType vt : VITALS) {
            ToDoubleFunction<MinuteRecord> ex = extractor.get(vt);

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
                    abnormalItems.add(new DailyReport.AbnormalItem(r.minuteStartMs(), vt, level, v));
                }
            }

            double avg = (n == 0) ? Double.NaN : (sum / n);
            if (n == 0) {
                min = Double.NaN;
                max = Double.NaN;
            }

            summaries.put(vt, new DailyReport.VitalSummary(min, max, avg, n, warn, urg));
        }

        // abnormal items: sort by time ascending (or latest first, you decide)
        abnormalItems.sort(Comparator.comparingLong(DailyReport.AbnormalItem::minuteStartMs));

        return new DailyReport(title, now, summaries, abnormalItems);
    }

    public String toText(DailyReport report, Patient patient) {
        StringBuilder sb = new StringBuilder();

        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        sb.append(report.title()).append("\n");
        sb.append("Generated at: ").append(dt.format(Instant.ofEpochMilli(report.generatedAtMs()))).append("\n\n");

        sb.append("Patient details:\n");
        sb.append("  ID: ").append(patient.patientId()).append("\n");
        sb.append("  Name: ").append(patient.name()).append("\n");
        sb.append("  Age: ").append(patient.age()).append("\n");
        sb.append("  Ward: ").append(patient.ward()).append("\n");
        sb.append("  Email: ").append(patient.email()).append("\n");
        sb.append("  Emergency contact: ").append(patient.emergencyContact()).append("\n\n");

        sb.append("1) Vital signs value range (minute averages)\n");
        sb.append("--------------------------------------------------\n");
        for (VitalType vt : List.of(
                VitalType.BODY_TEMPERATURE,
                VitalType.HEART_RATE,
                VitalType.RESPIRATORY_RATE,
                VitalType.SYSTOLIC_BP,
                VitalType.DIASTOLIC_BP
        )) {
            DailyReport.VitalSummary s = report.summaries().get(vt);
            sb.append(String.format("%-18s  min=%8.2f  max=%8.2f  avg=%8.2f  minutes=%4d  warning=%3d  urgent=%3d%n",
                    pretty(vt),
                    s.min(), s.max(), s.avg(),
                    s.totalMinutes(), s.warningMinutes(), s.urgentMinutes()));
        }
        sb.append("\n");

        sb.append("2) Abnormal details (minute-level)\n");
        sb.append("--------------------------------------------------\n");
        if (report.abnormalItems().isEmpty()) {
            sb.append("No abnormal minutes detected in the available data.\n");
        } else {
            for (DailyReport.AbnormalItem item : report.abnormalItems()) {
                sb.append(String.format("%s  %-18s  level=%-7s  value=%.2f%n",
                        dt.format(Instant.ofEpochMilli(item.minuteStartMs())),
                        pretty(item.vitalType()),
                        item.level(),
                        item.value()));
            }
        }

        return sb.toString();
    }

    private String pretty(VitalType vt) {
        return switch (vt) {
            case BODY_TEMPERATURE -> "Body temperature";
            case HEART_RATE -> "Heart rate";
            case RESPIRATORY_RATE -> "Respiratory rate";
            case SYSTOLIC_BP -> "Systolic BP";
            case DIASTOLIC_BP -> "Diastolic BP";
            default -> vt.name();
        };
    }
}
