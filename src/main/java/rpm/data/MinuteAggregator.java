package rpm.data;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates incoming samples into minute-level records and detects abnormal events.
 */

public class MinuteAggregator {
    private final AlertEngine alertEngine;

    private long currentMinuteStart = -1;
    private final List<VitalSample> bucket = new ArrayList<>();

    // Track the last alert level for each vital type to detect state changes.
    private AlertLevel lastTemp = AlertLevel.NORMAL;
    private AlertLevel lastHR   = AlertLevel.NORMAL;
    private AlertLevel lastRR   = AlertLevel.NORMAL;
    private AlertLevel lastSys  = AlertLevel.NORMAL;
    private AlertLevel lastDia  = AlertLevel.NORMAL;
    private AlertLevel lastECG  = AlertLevel.NORMAL;

    public MinuteAggregator(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
    }

    /**
     * Processes a single vital sample.
     * 1. Checks for immediate abnormal events (threshold violations).
     * 2. Aggregates data into 1-minute buckets.
     */
    public synchronized AggregationResult onSample(VitalSample s) {
        // 1. Detect abnormal events immediately
        List<AbnormalEvent> abnormalEvents = detectAbnormalEvents(s);

        // 2. Minute-level aggregation logic
        long minuteStart = (s.timestampMs() / 60000L) * 60000L;
        if (currentMinuteStart < 0) currentMinuteStart = minuteStart;

        // If we crossed a minute boundary, compute the average for the previous minute
        if (minuteStart != currentMinuteStart && !bucket.isEmpty()) {
            MinuteRecord rec = computeMinuteRecord(currentMinuteStart, bucket);

            bucket.clear();
            currentMinuteStart = minuteStart;
            bucket.add(s);

            return new AggregationResult(rec, abnormalEvents);
        }

        bucket.add(s);
        // If still in the same minute, return events only (record is null)
        return new AggregationResult(null, abnormalEvents);
    }

    /**
     * Checks all vital types against the AlertEngine.
     */
    private List<AbnormalEvent> detectAbnormalEvents(VitalSample s) {
        if (alertEngine == null) return Collections.emptyList();

        List<AbnormalEvent> events = new ArrayList<>();
        long now = s.timestampMs();

        // Use helper method 'check' to handle logic for each vital type
        lastTemp = check(events, now, VitalType.BODY_TEMPERATURE, s.bodyTemp(),        lastTemp);
        lastHR   = check(events, now, VitalType.HEART_RATE,       s.heartRate(),       lastHR);
        lastRR   = check(events, now, VitalType.RESPIRATORY_RATE, s.respiratoryRate(), lastRR);
        lastSys  = check(events, now, VitalType.SYSTOLIC_BP,      s.systolicBP(),      lastSys);
        lastDia  = check(events, now, VitalType.DIASTOLIC_BP,     s.diastolicBP(),     lastDia);

        // Optional: Check ECG if thresholds are defined
        lastECG  = check(events, now, VitalType.ECG,              s.ecgValue(),        lastECG);

        return events;
    }

    /**
     * Helper: Evaluates a value and detects state transitions.
     * This fixes the issue where upgrading from WARNING to URGENT was ignored.
     */
    private AlertLevel check(List<AbnormalEvent> events, long now, VitalType type, double val, AlertLevel lastLevel) {
        AlertLevel currentLevel = alertEngine.eval(type, val);

        // Logic: Record an event whenever the alert level CHANGES.
        // This ensures we capture transitions like WARNING -> URGENT, not just NORMAL -> ABNORMAL.
        if (currentLevel != lastLevel) {
            // Only record if the new state is not NORMAL (i.e., it's an anomaly)
            if (currentLevel != AlertLevel.NORMAL) {
                String msg = type + " value " + String.format("%.2f", val) + " is " + currentLevel;
                events.add(new AbnormalEvent(now, type, currentLevel, val, msg));
            }
        }
        return currentLevel; // Update the tracked level
    }

    /**
     * Computes the average of all samples in the bucket.
     */
    private MinuteRecord computeMinuteRecord(long minuteStart, List<VitalSample> samples) {
        int n = samples.size();

        double temp = samples.stream().mapToDouble(VitalSample::bodyTemp).average().orElse(Double.NaN);
        double hr   = samples.stream().mapToDouble(VitalSample::heartRate).average().orElse(Double.NaN);
        double rr   = samples.stream().mapToDouble(VitalSample::respiratoryRate).average().orElse(Double.NaN);
        double sys  = samples.stream().mapToDouble(VitalSample::systolicBP).average().orElse(Double.NaN);
        double dia  = samples.stream().mapToDouble(VitalSample::diastolicBP).average().orElse(Double.NaN);

        return new MinuteRecord(minuteStart, temp, hr, rr, sys, dia, n);
    }

    /**
     * Simple POJO to hold the result of the aggregation step.
     */
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
    }
}