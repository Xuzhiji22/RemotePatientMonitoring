package rpm.data;

public final class MinuteRecord {
    private final long minuteStartMs;
    private final double avgTemp;
    private final double avgHR;
    private final double avgRR;
    private final double avgSys;
    private final double avgDia;
    private final int sampleCount;

    public MinuteRecord(long minuteStartMs,
                        double avgTemp,
                        double avgHR,
                        double avgRR,
                        double avgSys,
                        double avgDia,
                        int sampleCount) {
        this.minuteStartMs = minuteStartMs;
        this.avgTemp = avgTemp;
        this.avgHR = avgHR;
        this.avgRR = avgRR;
        this.avgSys = avgSys;
        this.avgDia = avgDia;
        this.sampleCount = sampleCount;
    }

    public long getMinuteStartMs() {
        return minuteStartMs;
    }

    public double getAvgTemp() {
        return avgTemp;
    }

    public double getAvgHR() {
        return avgHR;
    }

    public double getAvgRR() {
        return avgRR;
    }

    public double getAvgSys() {
        return avgSys;
    }

    public double getAvgDia() {
        return avgDia;
    }

    public int getSampleCount() {
        return sampleCount;
    }
}
