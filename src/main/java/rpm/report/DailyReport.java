package rpm.report;

import rpm.model.AlertLevel;
import rpm.model.VitalType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DailyReport {

    private final String title;
    private final long generatedAtMs;
    private final Map<VitalType, VitalSummary> summaries;
    private final List<AbnormalItem> abnormalItems;

    public DailyReport(
            String title,
            long generatedAtMs,
            Map<VitalType, VitalSummary> summaries,
            List<AbnormalItem> abnormalItems
    ) {
        this.title = title;
        this.generatedAtMs = generatedAtMs;
        this.summaries = summaries;
        this.abnormalItems = abnormalItems;
    }

    public String title() {
        return title;
    }

    public long generatedAtMs() {
        return generatedAtMs;
    }

    public Map<VitalType, VitalSummary> summaries() {
        return summaries;
    }

    public List<AbnormalItem> abnormalItems() {
        return abnormalItems;
    }

    //Nested value classes


    public static final class VitalSummary {

        private final double min;
        private final double max;
        private final double avg;
        private final int totalMinutes;
        private final int warningMinutes;
        private final int urgentMinutes;

        public VitalSummary(
                double min,
                double max,
                double avg,
                int totalMinutes,
                int warningMinutes,
                int urgentMinutes
        ) {
            this.min = min;
            this.max = max;
            this.avg = avg;
            this.totalMinutes = totalMinutes;
            this.warningMinutes = warningMinutes;
            this.urgentMinutes = urgentMinutes;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        public double avg() {
            return avg;
        }

        public int totalMinutes() {
            return totalMinutes;
        }

        public int warningMinutes() {
            return warningMinutes;
        }

        public int urgentMinutes() {
            return urgentMinutes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VitalSummary)) return false;

            VitalSummary other = (VitalSummary) o;
            return Double.compare(other.min(), min) == 0
                    && Double.compare(other.max(), max) == 0
                    && Double.compare(other.avg(), avg) == 0
                    && totalMinutes == other.totalMinutes()
                    && warningMinutes == other.warningMinutes()
                    && urgentMinutes == other.urgentMinutes();
        }


        @Override
        public int hashCode() {
            return Objects.hash(min, max, avg, totalMinutes, warningMinutes, urgentMinutes);
        }

        @Override
        public String toString() {
            return "VitalSummary[" +
                    "min=" + min +
                    ", max=" + max +
                    ", avg=" + avg +
                    ", totalMinutes=" + totalMinutes +
                    ", warningMinutes=" + warningMinutes +
                    ", urgentMinutes=" + urgentMinutes +
                    "]";
        }
    }

    public static final class AbnormalItem {

        private final long minuteStartMs;
        private final VitalType vitalType;
        private final AlertLevel level;
        private final double value;

        public AbnormalItem(
                long minuteStartMs,
                VitalType vitalType,
                AlertLevel level,
                double value
        ) {
            this.minuteStartMs = minuteStartMs;
            this.vitalType = vitalType;
            this.level = level;
            this.value = value;
        }

        public long minuteStartMs() {
            return minuteStartMs;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbnormalItem)) return false;

            AbnormalItem other = (AbnormalItem) o;
            return minuteStartMs == other.minuteStartMs()
                    && Double.compare(other.value(), value) == 0
                    && vitalType == other.vitalType()
                    && level == other.level();
        }

        @Override
        public int hashCode() {
            return Objects.hash(minuteStartMs, vitalType, level, value);
        }

        @Override
        public String toString() {
            return "AbnormalItem[" +
                    "minuteStartMs=" + minuteStartMs +
                    ", vitalType=" + vitalType +
                    ", level=" + level +
                    ", value=" + value +
                    "]";
        }
    }

    /* =======================
       equals / hashCode / toString
       ======================= */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyReport)) return false;

        DailyReport other = (DailyReport) o;
        return generatedAtMs == other.generatedAtMs()
                && Objects.equals(title, other.title())
                && Objects.equals(summaries, other.summaries())
                && Objects.equals(abnormalItems, other.abnormalItems());
    }


    @Override
    public int hashCode() {
        return Objects.hash(title, generatedAtMs, summaries, abnormalItems);
    }

    @Override
    public String toString() {
        return "DailyReport[" +
                "title=" + title +
                ", generatedAtMs=" + generatedAtMs +
                ", summaries=" + summaries +
                ", abnormalItems=" + abnormalItems +
                "]";
    }
}
