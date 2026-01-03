package rpm.report;

import rpm.model.AlertLevel;
import rpm.model.VitalType;

import java.util.List;
import java.util.Map;

public final class DailyReport {

    private final String title;
    private final long generatedAtMs;
    private final Map<VitalType, VitalSummary> summaries;
    private final List<AbnormalItem> abnormalItems;

    public DailyReport(String title,
                       long generatedAtMs,
                       Map<VitalType, VitalSummary> summaries,
                       List<AbnormalItem> abnormalItems) {
        this.title = title;
        this.generatedAtMs = generatedAtMs;
        this.summaries = summaries;
        this.abnormalItems = abnormalItems;
    }

    public String getTitle() {
        return title;
    }

    public long getGeneratedAtMs() {
        return generatedAtMs;
    }

    public Map<VitalType, VitalSummary> getSummaries() {
        return summaries;
    }

    public List<AbnormalItem> getAbnormalItems() {
        return abnormalItems;
    }

    // ===== 原来嵌套的 record：VitalSummary =====
    public static final class VitalSummary {

        private final double min;
        private final double max;
        private final double avg;
        private final int totalMinutes;
        private final int warningMinutes;
        private final int urgentMinutes;

        public VitalSummary(double min,
                            double max,
                            double avg,
                            int totalMinutes,
                            int warningMinutes,
                            int urgentMinutes) {
            this.min = min;
            this.max = max;
            this.avg = avg;
            this.totalMinutes = totalMinutes;
            this.warningMinutes = warningMinutes;
            this.urgentMinutes = urgentMinutes;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getAvg() {
            return avg;
        }

        public int getTotalMinutes() {
            return totalMinutes;
        }

        public int getWarningMinutes() {
            return warningMinutes;
        }

        public int getUrgentMinutes() {
            return urgentMinutes;
        }
    }

    // ===== 原来嵌套的 record：AbnormalItem =====
    public static final class AbnormalItem {

        private final long minuteStartMs;
        private final VitalType vitalType;
        private final AlertLevel level;
        private final double value;

        public AbnormalItem(long minuteStartMs,
                            VitalType vitalType,
                            AlertLevel level,
                            double value) {
            this.minuteStartMs = minuteStartMs;
            this.vitalType = vitalType;
            this.level = level;
            this.value = value;
        }

        public long getMinuteStartMs() {
            return minuteStartMs;
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
    }
}
