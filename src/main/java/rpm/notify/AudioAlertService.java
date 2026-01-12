package rpm.notify;
import javax.sound.sampled.*;
import java.io.InputStream;
import rpm.data.AbnormalEvent;
import rpm.model.AlertLevel;

import java.awt.Toolkit;
import java.util.List;

/**
 * Very simple audio feedback:
 * - on abnormal events: beep once (WARNING) or three times (URGENT)
 * - optional heartbeat "tick": single beep on beat interval (based on HR)
 *
 * This uses Toolkit.beep() so it works on most OS without extra audio libs.
 */
public class AudioAlertService {

    private final boolean enabled;
    private final boolean heartbeatEnabled;

    // heartbeat scheduling
    private long nextBeatMs = 0;

    // throttle abnormal beeps to avoid spamming every sample
    private long lastAbnormalBeepMs = 0;
    private final long abnormalBeepCooldownMs = 1200; // 1.2s

    public AudioAlertService(boolean enabled, boolean heartbeatEnabled) {
        this.enabled = enabled;
        this.heartbeatEnabled = heartbeatEnabled;
    }

    public void onAbnormalEvents(List<AbnormalEvent> events, long nowMs) {
        if (!enabled) return;
        if (events == null || events.isEmpty()) return;

        // cooldown
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
            beep(3);
        } else if (hasWarning) {
            beep(1);
        }
    }

    public void onHeartRate(double hrBpm, long nowMs) {
        if (!enabled || !heartbeatEnabled) return;

        // ignore unreasonable HR
        if (Double.isNaN(hrBpm) || hrBpm < 30 || hrBpm > 220) return;

        long intervalMs = (long) (60000.0 / hrBpm);

        if (nextBeatMs == 0) {
            nextBeatMs = nowMs + intervalMs;
            return;
        }

        if (nowMs >= nextBeatMs) {
            beep(1);
            // schedule next beat (avoid drift a bit)
            nextBeatMs = nowMs + intervalMs;
        }
    }

    private void beep(int times) {
        for (int i = 0; i < times; i++) {
            playWav("/alert.wav");
            try {
                Thread.sleep(300);   // 间隔比 120ms 长一点，声音更清晰
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void playWav(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                System.err.println("Sound not found: " + resourcePath);
                return;
            }

            AudioInputStream audio = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



