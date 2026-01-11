package rpm;

import rpm.alert.AlertEngine;
import rpm.auth.AuthService;
import rpm.auth.UserStore;
import rpm.config.ConfigStore;
import rpm.dao.AbnormalEventDao;
import rpm.dao.MinuteAverageDao;
import rpm.data.PatientDataStore;
import rpm.data.PatientManager;
import rpm.data.MinuteAggregator;
import rpm.model.Patient;
import rpm.model.VitalSample;
import rpm.notify.AudioAlertService;
import rpm.notify.DailyDigestNotifier;
import rpm.notify.EmailService;
import rpm.notify.FileEmailService;
import rpm.sim.Simulator;
import rpm.ui.LoginFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        int maxSeconds = 300;
        int sampleHz = 5;
        long samplePeriodMs = 200;

        AlertEngine alertEngine = new AlertEngine();

        List<Patient> patients = List.of(
                new Patient("P1", "Patient 1", 34, "Ward A", "p1@hospital.com", "EC1"),
                new Patient("P2", "Patient 2", 56, "Ward A", "p2@hospital.com", "EC2"),
                new Patient("P3", "Patient 3", 41, "Ward B", "p3@hospital.com", "EC3"),
                new Patient("P4", "Patient 4", 28, "Ward B", "p4@hospital.com", "EC4"),
                new Patient("P5", "Patient 5", 73, "Ward C", "p5@hospital.com", "EC5"),
                new Patient("P6", "Patient 6", 62, "Ward C", "p6@hospital.com", "EC6"),
                new Patient("P7", "Patient 7", 39, "Ward D", "p7@hospital.com", "EC7"),
                new Patient("P8", "Patient 8", 45, "Ward D", "p8@hospital.com", "EC8")
        );

        PatientManager pm = new PatientManager(patients, maxSeconds, sampleHz, alertEngine);

        UserStore userStore = new UserStore(Paths.get("data", "users.properties"));
        ConfigStore configStore = new ConfigStore(Paths.get("data", "system.properties"));
        AuthService authService = new AuthService(userStore);

        // email digest config
        boolean emailEnabled = configStore.getBool("email.enabled", true);
        String doctorEmail = configStore.getString("doctor.email", "doctor@hospital.com");
        int digestHour = configStore.getInt("digest.hour", 8);
        int digestMinute = configStore.getInt("digest.minute", 0);

        EmailService emailService = new FileEmailService(Path.of("outbox"));
        DailyDigestNotifier digest = new DailyDigestNotifier(emailService, alertEngine, doctorEmail);

        // audio config (NEW)
        boolean audioEnabled = configStore.getBool("audio.enabled", true);
        boolean heartbeatEnabled = configStore.getBool("audio.heartbeat.enabled", false);
        AudioAlertService audioAlert = new AudioAlertService(audioEnabled, heartbeatEnabled);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        // DAO：只 new 一次复用
        MinuteAverageDao minuteDao = new MinuteAverageDao();
        AbnormalEventDao abnormalDao = new AbnormalEventDao();

        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Patient p : pm.getPatients()) {
                    String id = p.patientId();

                    Simulator sim = pm.simulatorOf(id);
                    PatientDataStore store = pm.storeOf(id);

                    VitalSample sample = sim.nextSample(now);
                    store.addSample(sample);

                    // optional heartbeat tick (based on incoming HR)
                    audioAlert.onHeartRate(sample.heartRate(), now);

                    MinuteAggregator agg = pm.aggregatorOf(id);
                    MinuteAggregator.AggregationResult result = agg.onSample(sample);

                    // 1) abnormal events：发生就写库 + 音频提示（不依赖 minuteRecord）
                    if (result.abnormalEvents() != null && !result.abnormalEvents().isEmpty()) {

                        // audio beep: WARNING=1, URGENT=3 (with cooldown)
                        audioAlert.onAbnormalEvents(result.abnormalEvents(), now);

                        for (var e : result.abnormalEvents()) {
                            try {
                                abnormalDao.insert(id, e);
                            } catch (Exception ex) {
                                System.err.println("abnormalDao.insert failed: " + ex.getMessage());
                            }
                        }
                    }

                    // 2) minute average：跨分钟才会产出
                    if (result.minuteRecord() != null) {
                        var mr = result.minuteRecord();
                        pm.historyOf(id).addMinuteRecord(mr);

                        try {
                            minuteDao.upsert(id, mr);
                        } catch (Exception ex) {
                            System.err.println("minuteDao.upsert failed: " + ex.getMessage());
                        }

                        if (emailEnabled) {
                            digest.onMinuteAverage(
                                    p,
                                    mr.minuteStartMs(),
                                    mr.avgTemp(),
                                    mr.avgHR(),
                                    mr.avgRR(),
                                    mr.avgSys(),
                                    mr.avgDia()
                            );
                        }
                    }
                }
            }
        }, 0, samplePeriodMs, TimeUnit.MILLISECONDS);

        // send digest once per day (check every 60s)
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!emailEnabled) return;

                LocalDateTime now = LocalDateTime.now();
                if (now.getHour() == digestHour && now.getMinute() == digestMinute) {
                    digest.sendDailyIfDue(now);
                }
            }
        }, 0, 60, TimeUnit.SECONDS);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginFrame login = new LoginFrame(pm, alertEngine, authService, userStore, configStore);
                login.setVisible(true);
            }
        });
    }
}
