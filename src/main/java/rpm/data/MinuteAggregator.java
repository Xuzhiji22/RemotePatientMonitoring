package rpm.data;

import rpm.alert.AlertEngine;
import rpm.model.VitalSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MinuteAggregator {
    private final AlertEngine alertEngine;

    private long currentMinuteStart = -1;
    private final List<VitalSample> bucket = new ArrayList<>();

    public MinuteAggregator(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
    }

    public synchronized AggregationResult onSample(VitalSample s) {
        long minuteStart = (s.timestampMs() / 60000L) * 60000L;

        if (currentMinuteStart < 0) currentMinuteStart = minuteStart;

        if (minuteStart != currentMinuteStart && !bucket.isEmpty()) {
            MinuteRecord rec = computeMinuteRecord(currentMinuteStart, bucket);
            bucket.clear();
            currentMinuteStart = minuteStart;
            bucket.add(s);
            return new AggregationResult(rec, List.of());
        }

        bucket.add(s);
        return new AggregationResult(null, List.of());
    }

    private MinuteRecord computeMinuteRecord(long minuteStart, List<VitalSample> samples) {
        int n = samples.size();
        double temp = samples.stream().mapToDouble(VitalSample::bodyTemp).average().orElse(Double.NaN);
        double hr   = samples.stream().mapToDouble(VitalSample::heartRate).average().orElse(Double.NaN);
        double rr   = samples.stream().mapToDouble(VitalSample::respiratoryRate).average().orElse(Double.NaN);
        double sys  = samples.stream().mapToDouble(VitalSample::systolicBP).average().orElse(Double.NaN);
        double dia  = samples.stream().mapToDouble(VitalSample::diastolicBP).average().orElse(Double.NaN);

        return new MinuteRecord(minuteStart, temp, hr, rr, sys, dia, n);
    }

    // Java 11 replacement for record AggregationResult(...)
    public static final class AggregationResult {
        private final MinuteRecord minuteRecord;
        private final List<AbnormalEvent> abnormalEvents;

        public AggregationResult(MinuteRecord minuteRecord, List<AbnormalEvent> abnormalEvents) {
            this.minuteRecord = minuteRecord;
            this.abnormalEvents = abnormalEvents;
        }

        public MinuteRecord minuteRecord() {
            return minuteRecord;
        }

        public List<AbnormalEvent> abnormalEvents() {
            return abnormalEvents;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AggregationResult)) return false;

            AggregationResult other = (AggregationResult) o;
            return Objects.equals(minuteRecord, other.minuteRecord())
                    && Objects.equals(abnormalEvents, other.abnormalEvents());
        }


        @Override
        public int hashCode() {
            return Objects.hash(minuteRecord, abnormalEvents);
        }

        @Override
        public String toString() {
            return "AggregationResult[" +
                    "minuteRecord=" + minuteRecord +
                    ", abnormalEvents=" + abnormalEvents +
                    "]";
        }
    }
}
