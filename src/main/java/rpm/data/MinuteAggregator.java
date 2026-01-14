package rpm.data;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MinuteAggregator {
    private final AlertEngine alertEngine;

    private long currentMinuteStart = -1;
    private final List<VitalSample> bucket = new ArrayList<>();


    private AlertLevel lastTemp = AlertLevel.NORMAL;
    private AlertLevel lastHR   = AlertLevel.NORMAL;
    private AlertLevel lastRR   = AlertLevel.NORMAL;
    private AlertLevel lastSys  = AlertLevel.NORMAL;
    private AlertLevel lastDia  = AlertLevel.NORMAL;
    private AlertLevel lastECG  = AlertLevel.NORMAL;

    public MinuteAggregator(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
    }


    public synchronized AggregationResult onSample(VitalSample s) {
        List<AbnormalEvent> abnormalEvents = detectAbnormalEvents(s);

        long minuteStart = (s.timestampMs() / 60000L) * 60000L;
        if (currentMinuteStart < 0) currentMinuteStart = minuteStart;

        if (minuteStart != currentMinuteStart && !bucket.isEmpty()) {
            MinuteRecord rec = computeMinuteRecord(currentMinuteStart, bucket);

            bucket.clear();
            currentMinuteStart = minuteStart;
            bucket.add(s);

            return new AggregationResult(rec, abnormalEvents);
        }

        bucket.add(s);
        return new AggregationResult(null, abnormalEvents);
    }

    private List<AbnormalEvent> detectAbnormalEvents(VitalSample s) {
        if (alertEngine == null) return Collections.emptyList();

        List<AbnormalEvent> out = new ArrayList<>();

        // Body Temp
        AlertLevel lt = alertEngine.eval(VitalType.BODY_TEMPERATURE, s.bodyTemp());
        if (lt != lastTemp) {
            if (lt != AlertLevel.NORMAL) out.add(new AbnormalEvent(s.timestampMs(), VitalType.BODY_TEMPERATURE, lt, s.bodyTemp(), "Body temperature out of range"));
            lastTemp = lt;
        }

        // Heart Rate
        AlertLevel lhr = alertEngine.eval(VitalType.HEART_RATE, s.heartRate());
        if (lhr != lastHR) {
            if (lhr != AlertLevel.NORMAL) out.add(new AbnormalEvent(s.timestampMs(), VitalType.HEART_RATE, lhr, s.heartRate(), "Heart rate out of range"));
            lastHR = lhr;
        }

        // Respiratory Rate
        AlertLevel lrr = alertEngine.eval(VitalType.RESPIRATORY_RATE, s.respiratoryRate());
        if (lrr != lastRR) {
            if (lrr != AlertLevel.NORMAL) out.add(new AbnormalEvent(s.timestampMs(), VitalType.RESPIRATORY_RATE, lrr, s.respiratoryRate(), "Respiratory rate out of range"));
            lastRR = lrr;
        }

        // Systolic BP
        AlertLevel lsys = alertEngine.eval(VitalType.SYSTOLIC_BP, s.systolicBP());
        if (lsys != lastSys) {
            if (lsys != AlertLevel.NORMAL) out.add(new AbnormalEvent(s.timestampMs(), VitalType.SYSTOLIC_BP, lsys, s.systolicBP(), "Systolic BP out of range"));
            lastSys = lsys;
        }

        // Diastolic BP
        AlertLevel ldia = alertEngine.eval(VitalType.DIASTOLIC_BP, s.diastolicBP());
        if (ldia != lastDia) {
            if (ldia != AlertLevel.NORMAL) out.add(new AbnormalEvent(s.timestampMs(), VitalType.DIASTOLIC_BP, ldia, s.diastolicBP(), "Diastolic BP out of range"));
            lastDia = ldia;
        }

        // ECG
        AlertLevel lecg = alertEngine.eval(VitalType.ECG, s.ecgValue());
        if (lecg != lastECG) {
            if (lecg != AlertLevel.NORMAL) out.add(new AbnormalEvent(s.timestampMs(), VitalType.ECG, lecg, s.ecgValue(), "ECG value out of range"));
            lastECG = lecg;
        }

        return out;
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
