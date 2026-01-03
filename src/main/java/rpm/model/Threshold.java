package rpm.model;

public final class Threshold {

    private final double warnLow;
    private final double warnHigh;
    private final double urgentLow;
    private final double urgentHigh;

    public Threshold(double warnLow,
                     double warnHigh,
                     double urgentLow,
                     double urgentHigh) {
        this.warnLow = warnLow;
        this.warnHigh = warnHigh;
        this.urgentLow = urgentLow;
        this.urgentHigh = urgentHigh;
    }

    public double getWarnLow() {
        return warnLow;
    }

    public double getWarnHigh() {
        return warnHigh;
    }

    public double getUrgentLow() {
        return urgentLow;
    }

    public double getUrgentHigh() {
        return urgentHigh;
    }

    public AlertLevel evaluate(double v) {
        if (v < urgentLow || v > urgentHigh) return AlertLevel.URGENT;
        if (v < warnLow || v > warnHigh) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }
}
