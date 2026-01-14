package rpm.cloud;

import rpm.data.AbnormalEvent;
import rpm.data.MinuteRecord;
import rpm.model.VitalSample;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudSyncService {

    private final boolean enabled;
    private final String baseUrl;
    private final int timeoutMs;
    private final long uploadPeriodMs;

    private final BlockingQueue<Item> q;
    private final ExecutorService worker;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // per patient last upload time (vitals only)
    private final ConcurrentHashMap<String, Long> lastVitalUploadMs = new ConcurrentHashMap<>();

    public CloudSyncService(boolean enabled,
                            String baseUrl,
                            int timeoutMs,
                            long uploadPeriodMs,
                            int queueMax) {
        this.enabled = enabled;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.timeoutMs = timeoutMs;
        this.uploadPeriodMs = uploadPeriodMs;
        this.q = new ArrayBlockingQueue<>(Math.max(100, queueMax));

        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "cloud-sync-worker");
            t.setDaemon(true);
            return t;
        });

        if (enabled) {
            worker.submit(this::loop);
        }
    }

    /** called by sampling thread: never block */
    public void enqueueVital(String patientId, VitalSample s) {
        if (!enabled || patientId == null || s == null) return;

        patientId = normalisePatientId(patientId);

        long now = System.currentTimeMillis();
        long last = lastVitalUploadMs.getOrDefault(patientId, 0L);
        if (now - last < uploadPeriodMs) return; // rate limit per patient
        lastVitalUploadMs.put(patientId, now);

        // if full -> drop silently
        q.offer(Item.vital(patientId, s));
    }

    /** called by sampling thread: never block */
    public void enqueueAbnormal(String patientId, AbnormalEvent e) {
        if (!enabled || patientId == null || e == null) return;

        patientId = normalisePatientId(patientId);

        // if full -> drop silently
        q.offer(Item.abnormal(patientId, e));
    }

    /** called by sampling thread: never block */
    public void enqueueMinuteRecord(String patientId, MinuteRecord r) {
        if (!enabled || patientId == null || r == null) return;

        patientId = normalisePatientId(patientId);

        // if full -> drop silently
        q.offer(Item.minute(patientId, r));
    }

    public void shutdown() {
        running.set(false);
        worker.shutdownNow();
    }

    private void loop() {
        while (running.get()) {
            try {
                Item it = q.poll(500, TimeUnit.MILLISECONDS);
                if (it == null) continue;

                if (it.kind == ItemKind.VITAL) {
                    postVital(it.patientId, it.vital);
                } else if (it.kind == ItemKind.ABNORMAL) {
                    postAbnormal(it.patientId, it.abnormal);
                } else if (it.kind == ItemKind.MINUTE) {
                    postMinute(it.patientId, it.minute);
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[cloud-sync] worker error: " + e.getMessage());
            }
        }
    }

    private void postVital(String patientId, VitalSample s) throws Exception {
        String json = "{"
                + "\"patientId\":\"" + escape(patientId) + "\","
                + "\"timestampMs\":" + s.timestampMs() + ","
                + "\"bodyTemp\":" + s.bodyTemp() + ","
                + "\"heartRate\":" + s.heartRate() + ","
                + "\"respiratoryRate\":" + s.respiratoryRate() + ","
                + "\"systolicBP\":" + s.systolicBP() + ","
                + "\"diastolicBP\":" + s.diastolicBP() + ","
                + "\"ecgValue\":" + s.ecgValue()
                + "}";

        httpPostJson(baseUrl + "/api/vitals", json, "POST /api/vitals");
    }

    private void postAbnormal(String patientId, AbnormalEvent e) throws Exception {
        String json = "{"
                + "\"patientId\":\"" + escape(patientId) + "\","
                + "\"timestampMs\":" + e.timestampMs() + ","
                + "\"vitalType\":\"" + escape(e.vitalType().name()) + "\","
                + "\"level\":\"" + escape(e.level().name()) + "\","
                + "\"value\":" + e.value() + ","
                + "\"message\":\"" + escape(e.message() == null ? "" : e.message()) + "\""
                + "}";

        httpPostJson(baseUrl + "/api/abnormal", json, "POST /api/abnormal");
    }

    private void postMinute(String patientId, MinuteRecord r) throws Exception {
        String json = "{"
                + "\"patientId\":\"" + escape(patientId) + "\","
                + "\"minuteStartMs\":" + r.minuteStartMs() + ","
                + "\"avgTemp\":" + r.avgTemp() + ","
                + "\"avgHR\":" + r.avgHR() + ","
                + "\"avgRR\":" + r.avgRR() + ","
                + "\"avgSys\":" + r.avgSys() + ","
                + "\"avgDia\":" + r.avgDia() + ","
                + "\"sampleCount\":" + r.sampleCount()
                + "}";

        httpPostJson(baseUrl + "/api/minutes", json, "POST /api/minutes");
    }

    private void httpPostJson(String urlStr, String json, String tag) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }

        int code = conn.getResponseCode();
        System.out.println("[cloud-sync] " + tag + " http=" + code);
        if (code < 200 || code >= 300) {
            System.err.println("[cloud-sync] " + tag + " failed http=" + code);
        }

        conn.disconnect();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private enum ItemKind {
        VITAL,
        ABNORMAL,
        MINUTE
    }

    private static class Item {
        final ItemKind kind;
        final String patientId;
        final VitalSample vital;
        final AbnormalEvent abnormal;
        final MinuteRecord minute;

        // ✅ 统一 5 参数构造器：所有 final 字段都能初始化
        private Item(ItemKind kind,
                     String patientId,
                     VitalSample vital,
                     AbnormalEvent abnormal,
                     MinuteRecord minute) {
            this.kind = kind;
            this.patientId = patientId;
            this.vital = vital;
            this.abnormal = abnormal;
            this.minute = minute;
        }

        static Item vital(String patientId, VitalSample s) {
            return new Item(ItemKind.VITAL, patientId, s, null, null);
        }

        static Item abnormal(String patientId, AbnormalEvent e) {
            return new Item(ItemKind.ABNORMAL, patientId, null, e, null);
        }

        static Item minute(String patientId, MinuteRecord r) {
            return new Item(ItemKind.MINUTE, patientId, null, null, r);
        }
    }

    /** Convert "P1" -> "P001". Keep already-normalised IDs as-is. */
    private static String normalisePatientId(String id) {
        if (id == null) return null;
        String t = id.trim();
        if (t.matches("P\\d{3}")) return t;
        if (t.matches("P\\d{1,2}")) {
            try {
                int n = Integer.parseInt(t.substring(1));
                return String.format("P%03d", n);
            } catch (Exception ignored) {
            }
        }
        return t;
    }
}
