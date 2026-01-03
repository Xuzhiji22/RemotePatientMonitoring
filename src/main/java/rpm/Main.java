package rpm;

import rpm.alert.AlertEngine;
import rpm.auth.AuthService;
import rpm.auth.UserStore;
import rpm.config.ConfigStore;
import rpm.data.PatientManager;
import rpm.model.Patient;
import rpm.ui.LoginFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
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

        // IMPORTANT: PatientManager constructor (with alertEngine)
        PatientManager pm = new PatientManager(patients, maxSeconds, sampleHz, alertEngine);

        // user + config storage (for admin features)
        UserStore userStore = new UserStore(Path.of("data/users.properties"));
        ConfigStore configStore = new ConfigStore(Path.of("data/system.properties"));
        AuthService authService = new AuthService(userStore);

        // background sampling: every 200ms
        var exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();

            for (Patient p : pm.getPatients()) {
                String id = p.patientId();

                var sim = pm.simulatorOf(id);
                var store = pm.storeOf(id);
                var sample = sim.nextSample(now);
                store.addSample(sample);

                // minute aggregation -> 24h history
                var agg = pm.aggregatorOf(id);
                var result = agg.onSample(sample);
                if (result.minuteRecord() != null) {
                    pm.historyOf(id).addMinuteRecord(result.minuteRecord());
                }
            }

        }, 0, samplePeriodMs, TimeUnit.MILLISECONDS);

        SwingUtilities.invokeLater(() -> {
            // use updated LoginFrame constructor
            LoginFrame login = new LoginFrame(pm, alertEngine, authService, userStore, configStore);
            login.setVisible(true);
        });
    }
}
