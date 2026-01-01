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

public abstract class BaseDetailFrame extends JFrame {

    protected final PatientManager pm;
    protected final AlertEngine alertEngine;
    protected final Patient patient;
    protected final JFrame overviewFrame;
    protected final JFrame loginFrame;

    private final VitalChartPanel tempPanel = VitalChartPanel.forVital("Body Temp (°C)", VitalType.BODY_TEMPERATURE, VitalSample::bodyTemp);
    private final VitalChartPanel hrPanel   = VitalChartPanel.forVital("Heart Rate (bpm)", VitalType.HEART_RATE, VitalSample::heartRate);
    private final VitalChartPanel rrPanel   = VitalChartPanel.forVital("Resp Rate (rpm)", VitalType.RESPIRATORY_RATE, VitalSample::respiratoryRate);
    private final VitalChartPanel sysPanel  = VitalChartPanel.forVital("Systolic BP (mmHg)", VitalType.SYSTOLIC_BP, VitalSample::systolicBP);
    private final VitalChartPanel diaPanel  = VitalChartPanel.forVital("Diastolic BP (mmHg)", VitalType.DIASTOLIC_BP, VitalSample::diastolicBP);
    private final EcgChartPanel ecgPanel    = new EcgChartPanel();

    public BaseDetailFrame(String title,
                           PatientManager pm,
                           AlertEngine alertEngine,
                           Patient patient,
                           JFrame overviewFrame,
                           JFrame loginFrame) {

        super(title);
        this.pm = pm;
        this.alertEngine = alertEngine;
        this.patient = patient;
        this.overviewFrame = overviewFrame;
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        JPanel left = buildPatientDetailsPanel(patient);

        // Top: Back + Logout + Controls
        JPanel top = new JPanel(new BorderLayout());

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton back = new JButton("Back");
        back.addActionListener(e -> goBack());

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> doLogout());

        leftTop.add(back);
        leftTop.add(logout);
        top.add(leftTop, BorderLayout.WEST);

        Simulator sim = pm.simulatorOf(patient.patientId());
        ControlsPanel controls = new ControlsPanel();
        controls.onWindowSecondsChanged(s -> {
            tempPanel.setWindowSeconds(s);
            hrPanel.setWindowSeconds(s);
            rrPanel.setWindowSeconds(s);
            sysPanel.setWindowSeconds(s);
            diaPanel.setWindowSeconds(s);
            ecgPanel.setWindowSeconds(Math.min(30, s));
        });
        controls.onModeChanged(sim::setMode);
        controls.onHrBaseChanged(sim::setHeartRateBase);
        top.add(controls, BorderLayout.CENTER);

        // Center: 2x3 grid
        JPanel centerGrid = new JPanel(new GridLayout(2, 3, 12, 12));
        centerGrid.add(ecgPanel);
        centerGrid.add(hrPanel);
        centerGrid.add(rrPanel);
        centerGrid.add(tempPanel);
        centerGrid.add(sysPanel);
        centerGrid.add(diaPanel);

        // Bottom: role-specific buttons (由子类决定)
        JPanel bottom = buildBottomPanel();

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(top, BorderLayout.NORTH);
        root.add(left, BorderLayout.WEST);
        root.add(centerGrid, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        new Timer(200, e -> refresh(tempPanel, hrPanel, rrPanel, sysPanel, diaPanel, ecgPanel)).start();
    }

    protected JPanel buildPatientDetailsPanel(Patient p) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(260, 600));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Patient Details"));

        panel.add(line("Ward: " + p.ward()));
        panel.add(line("Name: " + p.name()));
        panel.add(line("Age: " + p.age()));
        panel.add(line("Email: " + p.email()));
        panel.add(line("Emergency: " + p.emergencyContact()));

        panel.add(Box.createVerticalStrut(10));
        panel.add(line("View: " + viewName()));

        return panel;
    }

    protected JPanel line(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(text));
        return row;
    }

    private void refresh(VitalChartPanel temp, VitalChartPanel hr, VitalChartPanel rr,
                         VitalChartPanel sys, VitalChartPanel dia, EcgChartPanel ecg) {
        List<VitalSample> all = pm.storeOf(patient.patientId()).getBufferedSamples();
        if (all.isEmpty()) return;

        temp.updateData(all, alertEngine);
        hr.updateData(all, alertEngine);
        rr.updateData(all, alertEngine);
        sys.updateData(all, alertEngine);
        dia.updateData(all, alertEngine);
        ecg.updateData(all);
    }

    protected void goBack() {
        this.dispose();
        overviewFrame.setVisible(true);
    }

    protected void doLogout() {
        this.dispose();
        if (overviewFrame != null) overviewFrame.dispose();
        loginFrame.setVisible(true);
    }

    // 子类：底部按钮区怎么长
    protected abstract JPanel buildBottomPanel();

    // 子类：用于显示在左侧 patient details
    protected abstract String viewName();
}
