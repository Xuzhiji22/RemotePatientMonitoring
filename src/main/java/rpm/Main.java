package rpm;

import rpm.alert.AlertEngine;
import rpm.auth.AuthService;
import rpm.auth.UserStore;
import rpm.config.ConfigStore;
import rpm.data.MinuteAggregator;
import rpm.data.PatientDataStore;
import rpm.data.PatientManager;
import rpm.model.Patient;
import rpm.model.VitalSample;



import rpm.sim.Simulator;
import rpm.ui.LoginFrame;

import javax.swing.*;
import java.nio.file.Paths;
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

        // IMPORTANT: PatientManager constructor (with alertEngine)
        PatientManager pm = new PatientManager(patients, maxSeconds, sampleHz, alertEngine);

        // user + config storage (for admin features)
        UserStore userStore = new UserStore(Paths.get("data", "users.properties"));
        ConfigStore configStore = new ConfigStore(Paths.get("data", "system.properties"));
        AuthService authService = new AuthService(userStore);

        // background sampling: every 200ms
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
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

                    // minute aggregation -> 24h history
                    MinuteAggregator agg = pm.aggregatorOf(id);

                    /*
                     * 这里唯一不确定的是 onSample 的返回类型名字。
                     * 你把下面这一行里的 MinuteAggregatorResult 换成你项目真实返回类型即可
                     * （IDE 点到 onSample(...) 上会显示 return type）。
                     */
                    MinuteAggregator agg1 = pm.aggregatorOf(id);
                    MinuteAggregator.AggregationResult result = agg1.onSample(sample);

                    if (result.minuteRecord() != null) {
                        pm.historyOf(id).addMinuteRecord(result.minuteRecord());
                    }


                }
            }
        }, 0, samplePeriodMs, TimeUnit.MILLISECONDS);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginFrame login = new LoginFrame(pm, alertEngine, authService, userStore, configStore);
                login.setVisible(true);
            }
        });
    }
}
