package rpm.model;

import java.util.Objects;

public final class MinuteAverage {

    private final String patientId;
    private final long minuteStartMs;
    private final double avgTemp;
    private final double avgHr;
    private final double avgRr;
    private final double avgSys;
    private final double avgDia;
    private final int sampleCount;

    public MinuteAverage(String patientId, long minuteStartMs,
                         double avgTemp, double avgHr, double avgRr,
                         double avgSys, double avgDia, int sampleCount) {
        this.patientId = patientId;
        this.minuteStartMs = minuteStartMs;
        this.avgTemp = avgTemp;
        this.avgHr = avgHr;
        this.avgRr = avgRr;
        this.avgSys = avgSys;
        this.avgDia = avgDia;
        this.sampleCount = sampleCount;
    }

    public String patientId() { return patientId; }
    public long minuteStartMs() { return minuteStartMs; }
    public double avgTemp() { return avgTemp; }
    public double avgHr() { return avgHr; }
    public double avgRr() { return avgRr; }
    public double avgSys() { return avgSys; }
    public double avgDia() { return avgDia; }
    public int sampleCount() { return sampleCount; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinuteAverage)) return false;
        MinuteAverage other = (MinuteAverage) o;
        return minuteStartMs == other.minuteStartMs
                && Double.compare(other.avgTemp, avgTemp) == 0
                && Double.compare(other.avgHr, avgHr) == 0
                && Double.compare(other.avgRr, avgRr) == 0
                && Double.compare(other.avgSys, avgSys) == 0
                && Double.compare(other.avgDia, avgDia) == 0
                && sampleCount == other.sampleCount
                && Objects.equals(patientId, other.patientId);
    }

    @Override public int hashCode() {
        return Objects.hash(patientId, minuteStartMs, avgTemp, avgHr, avgRr, avgSys, avgDia, sampleCount);
    }
}
