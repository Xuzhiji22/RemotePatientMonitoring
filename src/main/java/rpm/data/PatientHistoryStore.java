package rpm.data;

import java.util.List;

public class PatientHistoryStore {
    // 24h * 60min = 1440
    private final MinuteRingBuffer<MinuteRecord> minuteRecords = new MinuteRingBuffer<>(1440);

    public void addMinuteRecord(MinuteRecord rec) {
        minuteRecords.add(rec);
    }

    public List<MinuteRecord> getLast24Hours() {
        return minuteRecords.snapshot(); // 最多 1440 条
    }
}
