package rpm.notify;

import rpm.data.AbnormalEvent;
import rpm.model.AlertLevel;
import rpm.model.VitalType;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Audio feedback:
 * - Abnormal events: WARNING -> 1 beep, URGENT -> 3 beeps (throttled)
 * - Heartbeat: periodic beep based on HR (optional)
 *
 * Notes:
 * - Runs audio playback asynchronously so sampling loop is not blocked.
 * - Uses alert.wav if available; falls back to Toolkit.beep() if audio fails.
 */
public class AudioAlertService {

    private final boolean enabled;
    private final boolean heartbeatEnabled;

    // heartbeat scheduling
    private long nextBeatMs = 0;

    // throttle abnormal beeps
    private long lastAbnormalBeepMs = 0;
    private final long abnormalBeepCooldownMs = 1200;

    // async audio executor (single thread keeps beep order)
    private final ExecutorService audioExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audio-alert");
        t.setDaemon(true);
        return t;
    });

    public AudioAlertService(boolean enabled, boolean heartbeatEnabled) {
        this.enabled = enabled;
        this.heartbeatEnabled = heartbeatEnabled;
    }

    public void onAbnormalEvents(List<AbnormalEvent> events, long nowMs) {
        if (!enabled) return;
        if (events == null || events.isEmpty()) return;

        if (nowMs - lastAbnormalBeepMs < abnormalBeepCooldownMs) return;
        lastAbnormalBeepMs = nowMs;

        boolean hasUrgent = false;
        boolean hasWarning = false;

        for (AbnormalEvent e : events) {
            if (e == null) continue;
            if (e.level() == AlertLevel.URGENT) hasUrgent = true;
            else if (e.level() == AlertLevel.WARNING) hasWarning = true;
        }

        if (hasUrgent) {
            beepAsync(3);
        } else if (hasWarning) {
            beepAsync(1);
        }
    }

    public void onHeartRate(double hrBpm, long nowMs) {
        if (!enabled || !heartbeatEnabled) return;

        if (Double.isNaN(hrBpm) || hrBpm < 30 || hrBpm > 220) return;

        long intervalMs = (long) (60000.0 / hrBpm);

        if (nextBeatMs == 0) {
            nextBeatMs = nowMs + intervalMs;
            return;
        }

        if (nowMs >= nextBeatMs) {
            beepAsync(1);
            nextBeatMs = nowMs + intervalMs;
        }
    }

    private void beepAsync(int times) {
        audioExec.submit(() -> {
            for (int i = 0; i < times; i++) {
                boolean ok = playWavOnce("/alert.wav");
                if (!ok) {
                    Toolkit.getDefaultToolkit().beep();
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    private boolean playWavOnce(String resourcePath) {
        try {
            InputStream is0 = getClass().getResourceAsStream(resourcePath);
            if (is0 == null) {
                System.err.println("[Audio] Sound not found: " + resourcePath);
                return false;
            }

            // BufferedInputStream improves compatibility on some JVM/OS
            try (InputStream is = new BufferedInputStream(is0);
                 AudioInputStream audio = AudioSystem.getAudioInputStream(is)) {

                Clip clip = AudioSystem.getClip();
                clip.open(audio);

                // auto-close when done
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Audio] play failed: " + e.getMessage());
            return false;
        }
    }
}
