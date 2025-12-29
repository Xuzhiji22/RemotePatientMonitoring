package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientDataStore;
import rpm.model.VitalSample;
import rpm.model.VitalType;
import rpm.sim.Simulator;
import rpm.sim.SimulationMode;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DoctorDashboardFrame extends JFrame {

    private final PatientDataStore store;
    private final AlertEngine alertEngine;
    private final Simulator simulator;

    private int windowSeconds = 30;

    private final VitalChartPanel tempPanel = VitalChartPanel.forVital("Body Temp (°C)", VitalType.BODY_TEMPERATURE, VitalSample::bodyTemp);
    private final VitalChartPanel hrPanel   = VitalChartPanel.forVital("Heart Rate (bpm)", VitalType.HEART_RATE, VitalSample::heartRate);
    private final VitalChartPanel rrPanel   = VitalChartPanel.forVital("Resp Rate (rpm)", VitalType.RESPIRATORY_RATE, VitalSample::respiratoryRate);
    private final VitalChartPanel sysPanel  = VitalChartPanel.forVital("Systolic BP (mmHg)", VitalType.SYSTOLIC_BP, VitalSample::systolicBP);
    private final VitalChartPanel diaPanel  = VitalChartPanel.forVital("Diastolic BP (mmHg)", VitalType.DIASTOLIC_BP, VitalSample::diastolicBP);
    private final EcgChartPanel ecgPanel    = new EcgChartPanel();

    public DoctorDashboardFrame(PatientDataStore store, AlertEngine alertEngine, Simulator simulator) {
        super("Remote Patient Monitor - Doctor");
        this.store = store;
        this.alertEngine = alertEngine;
        this.simulator = simulator;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        ControlsPanel controls = new ControlsPanel();
        controls.onWindowSecondsChanged(s -> {
            windowSeconds = s;
            tempPanel.setWindowSeconds(s);
            hrPanel.setWindowSeconds(s);
            rrPanel.setWindowSeconds(s);
            sysPanel.setWindowSeconds(s);
            diaPanel.setWindowSeconds(s);
            ecgPanel.setWindowSeconds(s);
        });

        controls.onModeChanged(mode -> simulator.setMode(mode));
        controls.onHrBaseChanged(bpm -> simulator.setHeartRateBase(bpm));

        JPanel grid = new JPanel(new GridLayout(2, 3, 10, 10));
        grid.add(tempPanel);
        grid.add(hrPanel);
        grid.add(rrPanel);
        grid.add(sysPanel);
        grid.add(diaPanel);
        grid.add(ecgPanel);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(controls, BorderLayout.NORTH);
        root.add(grid, BorderLayout.CENTER);

        setContentPane(root);

        // UI refresh timer
        new Timer(200, e -> refresh()).start();
    }

    private void refresh() {
        List<VitalSample> all = store.getBufferedSamples();
        if (all.isEmpty()) return;

        tempPanel.updateData(all, alertEngine);
        hrPanel.updateData(all, alertEngine);
        rrPanel.updateData(all, alertEngine);
        sysPanel.updateData(all, alertEngine);
        diaPanel.updateData(all, alertEngine);
        ecgPanel.updateData(all);
    }
}
