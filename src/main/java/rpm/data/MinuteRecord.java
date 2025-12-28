package rpm.data;

public record MinuteRecord(
        long minuteStartMs,
        double avgTemp,
        double avgHR,
        double avgRR,
        double avgSys,
        double avgDia,
        int sampleCount
) {}
