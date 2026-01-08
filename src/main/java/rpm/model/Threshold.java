package rpm.model;

import java.util.Objects;

public final class Threshold {

    private final double warnLow;
    private final double warnHigh;
    private final double urgentLow;
    private final double urgentHigh;

    public Threshold(
            double warnLow,
            double warnHigh,
            double urgentLow,
            double urgentHigh
    ) {
        this.warnLow = warnLow;
        this.warnHigh = warnHigh;
        this.urgentLow = urgentLow;
        this.urgentHigh = urgentHigh;
    }

    public double warnLow() {
        return warnLow;
    }

    public double warnHigh() {
        return warnHigh;
    }

    public double urgentLow() {
        return urgentLow;
    }

    public double urgentHigh() {
        return urgentHigh;
    }

    public AlertLevel evaluate(double v) {
        if (v < urgentLow || v > urgentHigh) return AlertLevel.URGENT;
        if (v < warnLow || v > warnHigh) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Threshold)) return false;

        Threshold other = (Threshold) o;
        return Double.compare(other.warnLow(), warnLow) == 0
                && Double.compare(other.warnHigh(), warnHigh) == 0
                && Double.compare(other.urgentLow(), urgentLow) == 0
                && Double.compare(other.urgentHigh(), urgentHigh) == 0;
    }


    @Override
    public int hashCode() {
        return Objects.hash(warnLow, warnHigh, urgentLow, urgentHigh);
    }

    @Override
    public String toString() {
        return "Threshold[" +
                "warnLow=" + warnLow +
                ", warnHigh=" + warnHigh +
                ", urgentLow=" + urgentLow +
                ", urgentHigh=" + urgentHigh +
                "]";
    }
}
