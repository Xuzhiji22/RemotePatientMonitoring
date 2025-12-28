package rpm;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;
import rpm.ui.OverviewFrame;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        int maxSeconds = 300;
        int sampleHz = 5;
        long samplePeriodMs = 200;

        AlertEngine alertEngine = new AlertEngine();

        // 8 个病人（你可以换成真实数据）
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

        PatientManager pm = new PatientManager(patients, maxSeconds, sampleHz);

        // 后台采样：每个病人都生成 sample
        var exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Patient p : pm.getPatients()) {
                var sim = pm.simulatorOf(p.patientId());
                var store = pm.storeOf(p.patientId());
                store.addSample(sim.nextSample(now));
            }
        }, 0, samplePeriodMs, TimeUnit.MILLISECONDS);

        SwingUtilities.invokeLater(() -> {
            OverviewFrame frame = new OverviewFrame(pm, alertEngine);
            frame.setVisible(true);
        });
    }
}
