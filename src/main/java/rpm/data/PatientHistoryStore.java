package rpm.data;

import java.util.List;

public class PatientHistoryStore {
    // 24h * 60min = 1440
    private final MinuteRingBuffer<MinuteRecord> minuteRecords = new MinuteRingBuffer<>(1440);

    public void addMinuteRecord(MinuteRecord rec) {
        minuteRecords.add(rec);
    }

    public List<MinuteRecord> getLast24Hours() {
        return minuteRecords.snapshot(); // max 1440
    }

    // for ui use
    public List<MinuteRecord> minuteRecords() {
        return getLast24Hours();
    }

    // for previous use
    public List<MinuteRecord> getMinuteRecords() {
        return getLast24Hours();
    }
}
