package rpm;

import rpm.alert.AlertEngine;
import rpm.auth.AuthService;
import rpm.auth.UserStore;
import rpm.config.ConfigStore;
import rpm.dao.AbnormalEventDao;
import rpm.dao.MinuteAverageDao;
import rpm.data.MinuteAggregator;
import rpm.data.PatientDataStore;
import rpm.data.PatientManager;
import rpm.model.Patient;
import rpm.model.VitalSample;
import rpm.notify.AudioAlertService;
import rpm.notify.DailyDigestNotifier;
import rpm.notify.EmailService;
import rpm.notify.FileEmailService;
import rpm.sim.Simulator;
import rpm.ui.LoginFrame;
import rpm.db.Db;
import rpm.cloud.CloudSyncService;
import rpm.notify.SmtpEmailService;
import java.util.concurrent.atomic.AtomicBoolean;





import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Level-3 friendly startup:
 * - On Tsuru (cloud): PG* env vars exist -> DB enabled -> writes to Postgres.
 * - On local machine: PG* usually missing -> DB auto-disabled -> no spam errors.
 *
 * UI still runs locally. DB persistence is only guaranteed when DB is enabled.
 */
public class Main {

    public static void main(String[] args) {
        int maxSeconds = 300;
        int sampleHz = 5;
        long samplePeriodMs = 200;

        AlertEngine alertEngine = new AlertEngine();

        List<Patient> patients = List.of(
                new Patient("P001", "Patient 1", 21, "Ward A", "patient1@example.com", "999-000-0001"),
                new Patient("P002", "Patient 2", 22, "Ward A", "patient2@example.com", "999-000-0002"),
                new Patient("P003", "Patient 3", 23, "Ward A", "patient3@example.com", "999-000-0003"),
                new Patient("P004", "Patient 4", 24, "Ward A", "patient4@example.com", "999-000-0004"),
                new Patient("P005", "Patient 5", 25, "Ward A", "patient5@example.com", "999-000-0005"),
                new Patient("P006", "Patient 6", 26, "Ward A", "patient6@example.com", "999-000-0006"),
                new Patient("P007", "Patient 7", 27, "Ward A", "patient7@example.com", "999-000-0007"),
                new Patient("P008", "Patient 8", 28, "Ward A", "patient8@example.com", "999-000-0008")
        );

        PatientManager pm = new PatientManager(patients, maxSeconds, sampleHz, alertEngine);

        // user + config storage
        UserStore userStore = new UserStore(Paths.get("data", "users.properties"));
        ConfigStore configStore = new ConfigStore(Paths.get("data", "system.properties"));

        AuthService authService = new AuthService(userStore);

        // ===== Email digest (optional) =====
        final boolean emailEnabled = configStore.getBool("email.enabled", true);
        final String doctorEmail = configStore.getString("doctor.email", "doctor@hospital.com");
        final int digestHour = configStore.getInt("digest.hour", 8);
        final int digestMinute = configStore.getInt("digest.minute", 0);

        // choose an email service: smtp or file
        final String emailMode = configStore.getString("email.mode", "file"); // "smtp" or "file"

        final EmailService emailService;
        if ("smtp".equalsIgnoreCase(emailMode)) {
            final String host = configStore.getString("smtp.host", "smtp.gmail.com");
            final int port = configStore.getInt("smtp.port", 587);
            final String user = configStore.getString("smtp.user", "");
            final String pass = configStore.getString("smtp.pass", "");
            final String from = configStore.getString("smtp.from", user);
            final boolean startTls = configStore.getBool("smtp.starttls", true);

            emailService = new SmtpEmailService(host, port, user, pass, startTls, from);
            System.out.println("[Main] email.mode=smtp host=" + host + ":" + port + " from=" + from);
        } else {
            final String outboxDir = configStore.getString("email.outboxDir", "outbox");
            emailService = new FileEmailService(Path.of(outboxDir));
            System.out.println("[Main] email.mode=file outboxDir=" + outboxDir);
        }

        DailyDigestNotifier digest = new DailyDigestNotifier(emailService, alertEngine, doctorEmail);


        // ===== Audio (optional) =====
        final boolean audioEnabledFromCfg = configStore.getBool("audio.enabled", false);
        final boolean heartbeatEnabled = configStore.getBool("audio.heartbeat.enabled", false);

        // ===== DB enabled flag (optional) =====
        // IMPORTANT: compute ONCE so it's effectively final for inner classes
        final boolean dbEnabled = configStore.getBool("db.enabled", true) && probeDatabaseOnce();

        // ===== Cloud sync (Level 3) =====
        final boolean cloudSyncEnabled = configStore.getBool("cloud.sync.enabled", false);
        final String cloudBaseUrl = configStore.getString("cloud.baseUrl", "https://bioeng-rpm-app.impaas.uk");
        final int cloudTimeoutMs = configStore.getInt("cloud.timeout.ms", 15000);
        final long cloudUploadPeriodMs = configStore.getInt("cloud.upload.period.ms", 1000);
        final int cloudQueueMax = configStore.getInt("cloud.queue.max", 2000);

        final CloudSyncService cloudSync =
                new CloudSyncService(cloudSyncEnabled, cloudBaseUrl, cloudTimeoutMs, cloudUploadPeriodMs, cloudQueueMax);

        System.out.println("[Main] cloud.sync.enabled=" + cloudSyncEnabled + " | cloud.baseUrl=" + cloudBaseUrl);

        // Audio should NOT depend on DB connectivity (requirement: audio alarms + heartbeat sound)
        final boolean audioEnabled = audioEnabledFromCfg;

        if (!dbEnabled) {
            System.out.println("[Main] DB not available -> running without DB. (Audio remains " + audioEnabled + ")");
        }

        final AudioAlertService audioAlert = new AudioAlertService(audioEnabled, heartbeatEnabled);
        final AtomicBoolean audioArmed = new AtomicBoolean(false); // NEW: only enable sound after login



        // DAO: create only if DB enabled
        final MinuteAverageDao minuteDao = dbEnabled ? new MinuteAverageDao() : null;
        final AbnormalEventDao abnormalDao = dbEnabled ? new AbnormalEventDao() : null;

        System.out.println("[Main] db.enabled=" + dbEnabled
                + " | email.enabled=" + emailEnabled
                + " | audio.enabled=" + audioEnabled
                + " | heartbeat.enabled=" + heartbeatEnabled);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();



        // background sampling: every 200ms
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
                    cloudSync.enqueueVital(id, sample);

                    MinuteAggregator agg = pm.aggregatorOf(id);
                    MinuteAggregator.AggregationResult result = agg.onSample(sample);

                    // Audio should only run after user login (avoid beeping on startup/login screen)
                    if (audioArmed.get()) {
                        audioAlert.onAbnormalEvents(result.abnormalEvents(), now);
                        audioAlert.onHeartRate(sample.heartRate(), now);
                    }


                    // 1) abnormal events: always try cloud upload; DB write only if enabled
                    if (result.abnormalEvents() != null && !result.abnormalEvents().isEmpty()) {
                        for (var e : result.abnormalEvents()) {

                            pm.historyOf(id).addAbnormalEvent(e);

                            // cloud (non-blocking)
                            cloudSync.enqueueAbnormal(id, e);

                            // DB (optional)
                            if (dbEnabled) {
                                try {
                                    abnormalDao.insert(id, e);
                                } catch (Exception ex) {
                                    // do not spam stack traces
                                    System.err.println("[Main] abnormalDao.insert failed: " + ex.getMessage());
                                }
                            }
                        }
                    }



                    // 2) minute average: write DB only if enabled
                    if (result.minuteRecord() != null) {
                        var mr = result.minuteRecord();
                        pm.historyOf(id).addMinuteRecord(mr);

                        // cloud (non-blocking)
                        cloudSync.enqueueMinuteRecord(id, mr);

                        if (dbEnabled) {
                            try {
                                minuteDao.upsert(id, mr);
                            } catch (Exception ex) {
                                System.err.println("[Main] minuteDao.upsert failed: " + ex.getMessage());
                            }
                        }

                        // digest stats (no immediate emails)
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

        // UI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginFrame login = new LoginFrame(
                        pm, alertEngine, authService, userStore, configStore,
                        () -> audioArmed.set(true),      // login -> arm
                        () -> {                          // logout/close -> disarm
                            audioArmed.set(false);
                            audioAlert.reset();          // 下面我会给你 AudioAlertService 的 reset()
                        }
                );


                login.setVisible(true);
            }
        });
    }

    /**
     * Try DB connection ONCE to decide whether DB should be enabled.
     * Avoids console spam when PG* env vars are missing locally.
     */
    private static boolean probeDatabaseOnce() {
        try (Connection c = Db.getConnection()) {
            return true;
        } catch (Exception e) {
            System.out.println("[Main] Database not available: " + e.getMessage());
            System.out.println("[Main] Tip: On Tsuru, bind Postgres service so PG* env vars are injected. " +
                    "Locally, set PG* env vars or set db.enabled=false.");
            return false;
        }
    }
}
