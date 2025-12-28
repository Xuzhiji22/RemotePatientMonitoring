package rpm.model;

public record Threshold(double warnLow, double warnHigh, double urgentLow, double urgentHigh) {

    public AlertLevel evaluate(double v) {
        if (v < urgentLow || v > urgentHigh) return AlertLevel.URGENT;
        if (v < warnLow || v > warnHigh) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }
}
