package rpm.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores recent minute-average records for UI browsing.
 *
 * Capacity is intentionally set to 7 days (7 * 24 * 60 minutes)
 * to support "view past week data" requirement.
 *
 * This is a local cache only. Long-term persistence is handled
 * by the cloud database.
 */
public class PatientHistoryStore {

    // 7 days * 24 hours * 60 minutes = 10080
    private static final int MAX_MINUTES = 7 * 24 * 60;

    private final MinuteRingBuffer<MinuteRecord> minuteRecords =
            new MinuteRingBuffer<>(MAX_MINUTES);

    /** Called by Main once per minute */
    public void addMinuteRecord(MinuteRecord rec) {
        minuteRecords.add(rec);
    }

    /** Last 24 hours (backward compatible) */
    public List<MinuteRecord> getLast24Hours() {
        return getLastHours(24);
    }

    /** Last N hours */
    public List<MinuteRecord> getLastHours(int hours) {
        return getLastMinutes(hours * 60);
    }

    /** Last N days */
    public List<MinuteRecord> getLastDays(int days) {
        return getLastMinutes(days * 24 * 60);
    }

    /** Core time-based filtering */
    private List<MinuteRecord> getLastMinutes(int minutes) {
        long now = System.currentTimeMillis();
        long fromMs = now - minutes * 60_000L;

        List<MinuteRecord> out = new ArrayList<>();
        for (MinuteRecord r : minuteRecords.snapshot()) {
            if (r.minuteStartMs() >= fromMs) {
                out.add(r);
            }
        }
        return out;
    }

    // ---- legacy accessors (keep to avoid breaking old UI code) ----

    /** for UI legacy use */
    public List<MinuteRecord> minuteRecords() {
        return getLast24Hours();
    }

    /** for previous use */
    public List<MinuteRecord> getMinuteRecords() {
        return getLast24Hours();
    }
}
