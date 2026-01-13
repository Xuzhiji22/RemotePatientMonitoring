package rpm.cloud;

import rpm.model.VitalSample;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking cloud uploader:
 * - Sampling thread: enqueue only (never blocked)
 * - Worker thread: POST to /api/vitals
 * - Per-patient rate limit (uploadPeriodMs)
 */
public class CloudSyncService {

    private final boolean enabled;
    private final String baseUrl;
    private final int timeoutMs;
    private final long uploadPeriodMs;

    private final BlockingQueue<Item> q;
    private final ExecutorService worker;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // per patient last upload time
    private final ConcurrentHashMap<String, Long> lastUploadMs = new ConcurrentHashMap<>();

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

        long now = System.currentTimeMillis();
        long last = lastUploadMs.getOrDefault(patientId, 0L);
        if (now - last < uploadPeriodMs) return; // rate limit per patient
        lastUploadMs.put(patientId, now);

        // offer: if queue full -> drop (avoid blocking sampling)
        boolean ok = q.offer(new Item(patientId, s));
        if (!ok) {
            // drop silently or print once; here keep minimal noise
        }
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
                postVital(it.patientId, it.sample);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[cloud-sync] worker error: " + e.getMessage());
            }
        }
    }

    private void postVital(String patientId, VitalSample s) throws Exception {
        // build minimal JSON (no gson needed)
        String json = "{" +
                "\"patientId\":\"" + escape(patientId) + "\"," +
                "\"timestampMs\":" + s.timestampMs() + "," +
                "\"bodyTemp\":" + s.bodyTemp() + "," +
                "\"heartRate\":" + s.heartRate() + "," +
                "\"respiratoryRate\":" + s.respiratoryRate() + "," +
                "\"systolicBP\":" + s.systolicBP() + "," +
                "\"diastolicBP\":" + s.diastolicBP() + "," +
                "\"ecgValue\":" + s.ecgValue() +
                "}";

        URL url = new URL(baseUrl + "/api/vitals");
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
        if (code < 200 || code >= 300) {
            System.err.println("[cloud-sync] POST /api/vitals http=" + code);
        }
        conn.disconnect();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class Item {
        final String patientId;
        final VitalSample sample;
        Item(String patientId, VitalSample sample) {
            this.patientId = patientId;
            this.sample = sample;
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
            } catch (Exception ignored) {}
        }
        return t;
    }
}
