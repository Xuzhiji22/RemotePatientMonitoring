package rpm.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatientHistoryStore {

    // 7d * 24h * 60min = 10080 minutes
    private final MinuteRingBuffer<MinuteRecord> minuteRecords = new MinuteRingBuffer<>(10080);

    // Abnormal events can be more frequent than minute records, give a larger buffer.
    // 20000 is usually enough for demos; increase if needed.
    private final MinuteRingBuffer<AbnormalEvent> abnormalEvents = new MinuteRingBuffer<>(20000);

    // -------- minute records --------
    public void addMinuteRecord(MinuteRecord rec) {
        minuteRecords.add(rec);
    }

    public List<MinuteRecord> getLast24Hours() {
        return getLastHours(24);
    }

    public List<MinuteRecord> getLastHours(int hours) {
        long now = System.currentTimeMillis();
        long from = now - hours * 3600_000L;
        return filterMinuteRecords(from, now);
    }

    public List<MinuteRecord> getLastDays(int days) {
        long now = System.currentTimeMillis();
        long from = now - days * 24L * 3600_000L;
        return filterMinuteRecords(from, now);
    }

    private List<MinuteRecord> filterMinuteRecords(long fromMs, long toMs) {
        List<MinuteRecord> all = minuteRecords.snapshot();
        if (all.isEmpty()) return List.of();

        List<MinuteRecord> out = new ArrayList<>();
        for (MinuteRecord r : all) {
            long t = r.minuteStartMs();
            if (t >= fromMs && t <= toMs) out.add(r);
        }
        return out;
    }

    // -------- abnormal events (真实事件) --------
    public void addAbnormalEvent(AbnormalEvent e) {
        abnormalEvents.add(e);
    }

    public List<AbnormalEvent> getAbnormalLastHours(int hours) {
        long now = System.currentTimeMillis();
        long from = now - hours * 3600_000L;
        return filterAbnormal(from, now);
    }

    public List<AbnormalEvent> getAbnormalLastDays(int days) {
        long now = System.currentTimeMillis();
        long from = now - days * 24L * 3600_000L;
        return filterAbnormal(from, now);
    }

    private List<AbnormalEvent> filterAbnormal(long fromMs, long toMs) {
        List<AbnormalEvent> all = abnormalEvents.snapshot();
        if (all.isEmpty()) return List.of();

        List<AbnormalEvent> out = new ArrayList<>();
        for (AbnormalEvent e : all) {
            long t = e.timestampMs();
            if (t >= fromMs && t <= toMs) out.add(e);
        }
        // UI一般希望最新在上：倒序
        Collections.reverse(out);
        return out;
    }

    // for ui legacy compatibility
    public List<MinuteRecord> minuteRecords() { return getLast24Hours(); }
    public List<MinuteRecord> getMinuteRecords() { return getLast24Hours(); }
}
