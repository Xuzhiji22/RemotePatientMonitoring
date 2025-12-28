package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;
import rpm.model.VitalSample;
import rpm.model.VitalType;
import rpm.sim.Simulator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DetailFrame extends JFrame {

    private final PatientManager pm;
    private final AlertEngine alertEngine;
    private final Patient patient;
    private final JFrame overviewFrame;

    private int windowSeconds = 30;

    private final VitalChartPanel tempPanel = VitalChartPanel.forVital("Body Temp (°C)", VitalType.BODY_TEMPERATURE, VitalSample::bodyTemp);
    private final VitalChartPanel hrPanel   = VitalChartPanel.forVital("Heart Rate (bpm)", VitalType.HEART_RATE, VitalSample::heartRate);
    private final VitalChartPanel rrPanel   = VitalChartPanel.forVital("Resp Rate (rpm)", VitalType.RESPIRATORY_RATE, VitalSample::respiratoryRate);
    private final VitalChartPanel sysPanel  = VitalChartPanel.forVital("Systolic BP (mmHg)", VitalType.SYSTOLIC_BP, VitalSample::systolicBP);
    private final VitalChartPanel diaPanel  = VitalChartPanel.forVital("Diastolic BP (mmHg)", VitalType.DIASTOLIC_BP, VitalSample::diastolicBP);
    private final EcgChartPanel ecgPanel    = new EcgChartPanel();

    public DetailFrame(PatientManager pm, AlertEngine alertEngine, Patient patient, JFrame overviewFrame) {
        super("Patient Detail - " + patient.patientId());
        this.pm = pm;
        this.alertEngine = alertEngine;
        this.patient = patient;
        this.overviewFrame = overviewFrame;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        // Left: patient details
        JPanel left = buildPatientDetailsPanel(patient);

        // Top controls + Back
        JPanel top = new JPanel(new BorderLayout());
        JButton back = new JButton("Back");
        back.addActionListener(e -> goBack());
        top.add(back, BorderLayout.WEST);

        Simulator sim = pm.simulatorOf(patient.patientId());
        ControlsPanel controls = new ControlsPanel();
        controls.onWindowSecondsChanged(this::applyWindowSeconds);
        controls.onModeChanged(sim::setMode);
        controls.onHrBaseChanged(sim::setHeartRateBase);
        top.add(controls, BorderLayout.CENTER);

        // Center layout like your sketch
        JPanel centerGrid = new JPanel(new GridLayout(2, 3, 12, 12));
        centerGrid.add(ecgPanel);
        centerGrid.add(hrPanel);
        centerGrid.add(rrPanel);
        centerGrid.add(tempPanel);
        centerGrid.add(sysPanel);
        centerGrid.add(diaPanel);

        // Bottom buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        JButton viewPast = new JButton("View Past Report");
        JButton pastAbn  = new JButton("Past Abnormal");
        JButton genRep   = new JButton("Generate Report");
        bottom.add(viewPast);
        bottom.add(pastAbn);
        bottom.add(genRep);

        // Root
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(top, BorderLayout.NORTH);
        root.add(left, BorderLayout.WEST);
        root.add(centerGrid, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        // refresh timer
        new Timer(200, e -> refresh()).start();
    }

    private JPanel buildPatientDetailsPanel(Patient p) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(260, 600));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Patient Details"));

        panel.add(line("Ward: " + p.ward()));
        panel.add(line("Name: " + p.name()));
        panel.add(line("Age: " + p.age()));
        panel.add(line("Email: " + p.email()));
        panel.add(line("Emergency: " + p.emergencyContact()));

        return panel;
    }

    private JPanel line(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(text));
        return row;
    }

    private void applyWindowSeconds(int s) {
        this.windowSeconds = s;
        tempPanel.setWindowSeconds(s);
        hrPanel.setWindowSeconds(s);
        rrPanel.setWindowSeconds(s);
        sysPanel.setWindowSeconds(s);
        diaPanel.setWindowSeconds(s);
        ecgPanel.setWindowSeconds(Math.min(30, s)); // ECG 建议窗口别太长
    }

    private void refresh() {
        List<VitalSample> all = pm.storeOf(patient.patientId()).getBufferedSamples();
        if (all.isEmpty()) return;

        tempPanel.updateData(all, alertEngine);
        hrPanel.updateData(all, alertEngine);
        rrPanel.updateData(all, alertEngine);
        sysPanel.updateData(all, alertEngine);
        diaPanel.updateData(all, alertEngine);
        ecgPanel.updateData(all);
    }

    private void goBack() {
        this.dispose();
        overviewFrame.setVisible(true);
    }
}
