package rpm.data;

import java.util.Objects;

public final class MinuteRecord {

    private final long minuteStartMs;
    private final double avgTemp;
    private final double avgHR;
    private final double avgRR;
    private final double avgSys;
    private final double avgDia;
    private final int sampleCount;

    public MinuteRecord(
            long minuteStartMs,
            double avgTemp,
            double avgHR,
            double avgRR,
            double avgSys,
            double avgDia,
            int sampleCount
    ) {
        this.minuteStartMs = minuteStartMs;
        this.avgTemp = avgTemp;
        this.avgHR = avgHR;
        this.avgRR = avgRR;
        this.avgSys = avgSys;
        this.avgDia = avgDia;
        this.sampleCount = sampleCount;
    }

    public long minuteStartMs() {
        return minuteStartMs;
    }

    public double avgTemp() {
        return avgTemp;
    }

    public double avgHR() {
        return avgHR;
    }

    public double avgRR() {
        return avgRR;
    }

    public double avgSys() {
        return avgSys;
    }

    public double avgDia() {
        return avgDia;
    }

    public int sampleCount() {
        return sampleCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinuteRecord)) return false;

        MinuteRecord other = (MinuteRecord) o;
        return minuteStartMs == other.minuteStartMs()
                && Double.compare(other.avgTemp(), avgTemp) == 0
                && Double.compare(other.avgHR(), avgHR) == 0
                && Double.compare(other.avgRR(), avgRR) == 0
                && Double.compare(other.avgSys(), avgSys) == 0
                && Double.compare(other.avgDia(), avgDia) == 0
                && sampleCount == other.sampleCount();
    }


    @Override
    public int hashCode() {
        return Objects.hash(
                minuteStartMs,
                avgTemp,
                avgHR,
                avgRR,
                avgSys,
                avgDia,
                sampleCount
        );
    }

    @Override
    public String toString() {
        return "MinuteRecord[" +
                "minuteStartMs=" + minuteStartMs +
                ", avgTemp=" + avgTemp +
                ", avgHR=" + avgHR +
                ", avgRR=" + avgRR +
                ", avgSys=" + avgSys +
                ", avgDia=" + avgDia +
                ", sampleCount=" + sampleCount +
                "]";
    }
}
