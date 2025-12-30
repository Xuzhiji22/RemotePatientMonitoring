package rpm.report;

import rpm.model.AlertLevel;
import rpm.model.VitalType;

import java.util.List;
import java.util.Map;

public record DailyReport(
        String title,
        long generatedAtMs,
        Map<VitalType, VitalSummary> summaries,
        List<AbnormalItem> abnormalItems
) {
    public record VitalSummary(
            double min,
            double max,
            double avg,
            int totalMinutes,
            int warningMinutes,
            int urgentMinutes
    ) {}

    public record AbnormalItem(
            long minuteStartMs,
            VitalType vitalType,
            AlertLevel level,
            double value
    ) {}
}
