package rpm.alert;

import rpm.model.AlertLevel;
import rpm.model.Threshold;
import rpm.model.VitalType;

import java.util.EnumMap;
import java.util.Map;

public class AlertEngine {
    private final Map<VitalType, Threshold> thresholds = new EnumMap<>(VitalType.class);

    public AlertEngine() {
        thresholds.put(VitalType.BODY_TEMPERATURE, new Threshold(36.0, 37.5, 35.0, 39.0));
        thresholds.put(VitalType.HEART_RATE,       new Threshold(50, 100, 40, 140));
        thresholds.put(VitalType.RESPIRATORY_RATE, new Threshold(10, 20, 6, 30));
        thresholds.put(VitalType.SYSTOLIC_BP,      new Threshold(90, 140, 70, 180));
        thresholds.put(VitalType.DIASTOLIC_BP,     new Threshold(60, 90, 40, 120));
        // ECG 通常不按这个阈值报警，UI里可以忽略或自定义
        thresholds.put(VitalType.ECG,              new Threshold(-999, 999, -999, 999));
    }

    public AlertLevel eval(VitalType type, double value) {
        Threshold t = thresholds.get(type);
        if (t == null) return AlertLevel.NORMAL;
        return t.evaluate(value);
    }

    public Threshold getThreshold(VitalType type) {
        return thresholds.get(type);
    }

    public void setThreshold(VitalType type, Threshold t) {
        thresholds.put(type, t);
    }
}
