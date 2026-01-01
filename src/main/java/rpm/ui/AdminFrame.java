package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Threshold;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;

public class AdminFrame extends JFrame {

    private final PatientManager pm;
    private final AlertEngine alertEngine;
    private final JFrame loginFrame;

    public AdminFrame(PatientManager pm, AlertEngine alertEngine, JFrame loginFrame) {
        super("Administrator Console");
        this.pm = pm;
        this.alertEngine = alertEngine;
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            this.dispose();
            loginFrame.setVisible(true);
        });

        JButton openMonitor = new JButton("Open Monitoring Overview");
        openMonitor.addActionListener(e -> {
            // IMPORTANT: BaseOverviewFrame is abstract -> cannot new it.
            // Admin opens the monitoring page using DoctorOverviewFrame (read-only monitor view)
            DoctorOverviewFrame ov = new DoctorOverviewFrame(pm, alertEngine, loginFrame);
            ov.setVisible(true);
            this.setVisible(false);
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.add(logout);
        top.add(openMonitor);

        JPanel thresholdsPanel = buildThresholdsPanel(alertEngine);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(top, BorderLayout.NORTH);
        root.add(thresholdsPanel, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildThresholdsPanel(AlertEngine alertEngine) {
        // 简化版：先用 GridLayout 显示 5 行阈值编辑
        // 如果你想要每行一个 Apply 按钮，我们再改成 GridBagLayout
        JPanel panel = new JPanel(new GridLayout(6, 5, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Configure Alert Thresholds (warning / urgent)"));

        panel.add(new JLabel("Vital"));
        panel.add(new JLabel("warnLow"));
        panel.add(new JLabel("warnHigh"));
        panel.add(new JLabel("urgentLow"));
        panel.add(new JLabel("urgentHigh"));

        addRow(panel, alertEngine, VitalType.BODY_TEMPERATURE, 0.01);
        addRow(panel, alertEngine, VitalType.HEART_RATE, 1.0);
        addRow(panel, alertEngine, VitalType.RESPIRATORY_RATE, 1.0);
        addRow(panel, alertEngine, VitalType.SYSTOLIC_BP, 1.0);
        addRow(panel, alertEngine, VitalType.DIASTOLIC_BP, 1.0);

        return panel;
    }

    private void addRow(JPanel panel, AlertEngine alertEngine, VitalType type, double step) {
        Threshold t = alertEngine.getThreshold(type);

        JLabel name = new JLabel(type.name());

        JSpinner warnLow  = new JSpinner(new SpinnerNumberModel(t.warnLow(), -9999.0, 9999.0, step));
        JSpinner warnHigh = new JSpinner(new SpinnerNumberModel(t.warnHigh(), -9999.0, 9999.0, step));
        JSpinner urgLow   = new JSpinner(new SpinnerNumberModel(t.urgentLow(), -9999.0, 9999.0, step));
        JSpinner urgHigh  = new JSpinner(new SpinnerNumberModel(t.urgentHigh(), -9999.0, 9999.0, step));

        // 这里我们做成“改动即生效”：spinner 改了就更新 alertEngine
        // 如果你更想要显式 Apply 按钮，我也可以给你改。
        warnLow.addChangeListener(e -> update(alertEngine, type, warnLow, warnHigh, urgLow, urgHigh));
        warnHigh.addChangeListener(e -> update(alertEngine, type, warnLow, warnHigh, urgLow, urgHigh));
        urgLow.addChangeListener(e -> update(alertEngine, type, warnLow, warnHigh, urgLow, urgHigh));
        urgHigh.addChangeListener(e -> update(alertEngine, type, warnLow, warnHigh, urgLow, urgHigh));

        panel.add(name);
        panel.add(warnLow);
        panel.add(warnHigh);
        panel.add(urgLow);
        panel.add(urgHigh);
    }

    private void update(AlertEngine alertEngine,
                        VitalType type,
                        JSpinner warnLow, JSpinner warnHigh,
                        JSpinner urgLow, JSpinner urgHigh) {

        Threshold nt = new Threshold(
                ((Number) warnLow.getValue()).doubleValue(),
                ((Number) warnHigh.getValue()).doubleValue(),
                ((Number) urgLow.getValue()).doubleValue(),
                ((Number) urgHigh.getValue()).doubleValue()
        );
        alertEngine.setThreshold(type, nt);
    }
}
