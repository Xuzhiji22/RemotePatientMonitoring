package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.AlertLevel;
import rpm.model.Patient;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PatientTilePanel extends JPanel {

    private Runnable openDetail = () -> {};
    private final Patient patient;
    private final PatientManager pm;
    private final AlertEngine alertEngine;

    private final EcgChartPanel ecgPanel = new EcgChartPanel();
    private final JLabel title = new JLabel();
    private final JLabel status = new JLabel();

    public PatientTilePanel(Patient patient, PatientManager pm, AlertEngine alertEngine) {
        this.patient = patient;
        this.pm = pm;
        this.alertEngine = alertEngine;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        setBackground(Color.WHITE);

        title.setText(patient.patientId() + " - " + patient.name());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        status.setText("Status: --");
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.add(title);
        header.add(status);

        ecgPanel.setPreferredSize(new Dimension(250, 160));
        ecgPanel.setWindowSeconds(10);

        add(header, BorderLayout.NORTH);
        add(ecgPanel, BorderLayout.CENTER);

        JButton open = new JButton("Open");
        open.addActionListener(e -> openDetail.run());
        add(open, BorderLayout.SOUTH);

        // 定时刷新 tile（ECG + 状态）
        new Timer(300, e -> refresh()).start();

        // 点击卡片也能打开
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { openDetail.run(); }
        });
    }

    public void onOpenDetail(Runnable r) {
        this.openDetail = r;
    }

    private void refresh() {
        List<VitalSample> all = pm.storeOf(patient.patientId()).getBufferedSamples();
        if (all.isEmpty()) return;

        ecgPanel.updateData(all);

        VitalSample last = all.get(all.size() - 1);
        AlertLevel worst = worstLevel(last);

        status.setText("Status: " + worst);
        setBorder(BorderFactory.createLineBorder(levelColor(worst), 4));
        repaint();
    }

    private AlertLevel worstLevel(VitalSample last) {
        AlertLevel a = alertEngine.eval(VitalType.BODY_TEMPERATURE, last.bodyTemp());
        AlertLevel b = alertEngine.eval(VitalType.HEART_RATE, last.heartRate());
        AlertLevel c = alertEngine.eval(VitalType.RESPIRATORY_RATE, last.respiratoryRate());
        AlertLevel d = alertEngine.eval(VitalType.SYSTOLIC_BP, last.systolicBP());
        AlertLevel e = alertEngine.eval(VitalType.DIASTOLIC_BP, last.diastolicBP());
        return max(a, max(b, max(c, max(d, e))));
    }

    private AlertLevel max(AlertLevel x, AlertLevel y) {
        if (x == AlertLevel.URGENT || y == AlertLevel.URGENT) return AlertLevel.URGENT;
        if (x == AlertLevel.WARNING || y == AlertLevel.WARNING) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }

    private Color levelColor(AlertLevel level) {
        switch (level) {
            case NORMAL:
                return new Color(0, 170, 0);
            case WARNING:
                return new Color(230, 170, 0);
            case URGENT:
                return new Color(220, 0, 0);
            default:
                return Color.BLACK;
        }
    }

}
