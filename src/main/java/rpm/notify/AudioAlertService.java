package rpm.notify;

import rpm.data.AbnormalEvent;
import rpm.model.AlertLevel;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioAlertService {

    private final boolean enabled;
    private final boolean heartbeatEnabled;

    // heartbeat scheduling
    private long nextBeatMs = 0;

    // throttle abnormal beeps
    private long lastAbnormalBeepMs = 0;
    private final long abnormalBeepCooldownMs = 1200;

    // when alarm plays, suppress heartbeat briefly to avoid overlap
    private volatile long suppressHeartbeatUntilMs = 0;

    // async executor so sampling loop is never blocked
    private final ExecutorService audioExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audio-alert");
        t.setDaemon(true);
        return t;
    });

    public AudioAlertService(boolean enabled, boolean heartbeatEnabled) {
        this.enabled = enabled;
        this.heartbeatEnabled = heartbeatEnabled;
    }

    /** Call on logout to stop immediate “carry over” beeps */
    public void reset() {
        nextBeatMs = 0;
        lastAbnormalBeepMs = 0;
        suppressHeartbeatUntilMs = 0;
    }

    /** WARNING/URGENT: alarm sound only (distinct from heartbeat) */
    public void onAbnormalEvents(List<AbnormalEvent> events, long nowMs) {
        if (!enabled) return;
        if (events == null || events.isEmpty()) return;

        // cooldown
        if (nowMs - lastAbnormalBeepMs < abnormalBeepCooldownMs) return;
        lastAbnormalBeepMs = nowMs;

        boolean hasUrgent = false;
        boolean hasWarning = false;
        for (AbnormalEvent e : events) {
            if (e == null || e.level() == null) continue;
            if (e.level() == AlertLevel.URGENT) hasUrgent = true;
            else if (e.level() == AlertLevel.WARNING) hasWarning = true;
        }

        if (!hasUrgent && !hasWarning) return;

        // pause heartbeat briefly to avoid overlapping sounds
        suppressHeartbeatUntilMs = nowMs + 1200;

        if (hasUrgent) {
            playAlarmAsync(3);
        } else {
            playAlarmAsync(1);
        }
    }

    /** Normal: heartbeat only (distinct) */
    public void onHeartRate(double hrBpm, long nowMs) {
        if (!enabled || !heartbeatEnabled) return;

        // suppressed while alarm is playing
        if (nowMs < suppressHeartbeatUntilMs) return;

        if (Double.isNaN(hrBpm) || hrBpm < 30 || hrBpm > 220) return;

        long intervalMs = (long) (60000.0 / hrBpm);

        if (nextBeatMs == 0) {
            nextBeatMs = nowMs + intervalMs;
            return;
        }

        if (nowMs >= nextBeatMs) {
            playHeartbeatAsync();
            nextBeatMs = nowMs + intervalMs;
        }
    }

    private void playAlarmAsync(int times) {
        audioExec.submit(() -> {
            for (int i = 0; i < times; i++) {
                // Use /alarm.wav if available; fallback beep
                if (!playWavOnce("/alert.wav")) Toolkit.getDefaultToolkit().beep();
                sleepQuiet(250);
            }
        });
    }

    private void playHeartbeatAsync() {
        audioExec.submit(() -> {
            // Use /heartbeat.wav if available; fallback beep
            if (!playWavOnce("/beep.wav")) Toolkit.getDefaultToolkit().beep();
        });
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private boolean playWavOnce(String resourcePath) {
        try {
            InputStream is0 = getClass().getResourceAsStream(resourcePath);
            if (is0 == null) return false;

            try (InputStream is = new BufferedInputStream(is0);
                 AudioInputStream audio = AudioSystem.getAudioInputStream(is)) {

                Clip clip = AudioSystem.getClip();
                clip.open(audio);

                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
