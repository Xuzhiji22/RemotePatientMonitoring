package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VitalTablePanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;
    private final JSpinner pageSizeSpinner;
    private final JCheckBox abnormalOnly;
    private final JLabel info;

    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public VitalTablePanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.add(new JLabel("Page size:"));

        pageSizeSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 300, 5));
        top.add(pageSizeSpinner);

        abnormalOnly = new JCheckBox("Abnormal only");
        top.add(abnormalOnly);

        JButton refreshBtn = new JButton("Refresh");
        top.add(refreshBtn);

        info = new JLabel(" ");
        info.setForeground(Color.GRAY);
        top.add(info);

        model = new DefaultTableModel(new Object[]{
                "Time", "Temp (°C)", "HR (bpm)", "RR (rpm)", "BP (sys/dia)", "Status"
        }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);

        refreshBtn.addActionListener(e ->
                info.setText("Refreshed at " + fmt.format(new Date()))
        );

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setSamples(List<VitalSample> all, AlertEngine alertEngine) {
        int pageSize = (Integer) pageSizeSpinner.getValue();
        boolean onlyAbn = abnormalOnly.isSelected();

        model.setRowCount(0);

        // newest first
        int shown = 0;
        for (int i = all.size() - 1; i >= 0 && shown < pageSize; i--) {
            VitalSample s = all.get(i);

            AlertLevel status = worstStatus(alertEngine, s);
            if (onlyAbn && status == AlertLevel.NORMAL) continue;

            model.addRow(new Object[] {
                    fmt.format(new Date(s.timestampMs())),
                    String.format("%.2f", s.bodyTemp()),
                    String.format("%.2f", s.heartRate()),
                    String.format("%.2f", s.respiratoryRate()),
                    String.format("%.0f/%.0f", s.systolicBP(), s.diastolicBP()),
                    status.toString()
            });

            shown++;
        }


        info.setText("Showing " + shown + " record(s) (newest first)");
    }

    private AlertLevel worstStatus(AlertEngine alertEngine, VitalSample s) {
        AlertLevel t   = alertEngine.eval(VitalType.BODY_TEMPERATURE, s.bodyTemp());
        AlertLevel h   = alertEngine.eval(VitalType.HEART_RATE, s.heartRate());
        AlertLevel r   = alertEngine.eval(VitalType.RESPIRATORY_RATE, s.respiratoryRate());
        AlertLevel sys = alertEngine.eval(VitalType.SYSTOLIC_BP, s.systolicBP());
        AlertLevel dia = alertEngine.eval(VitalType.DIASTOLIC_BP, s.diastolicBP());

        AlertLevel worst = t;
        worst = moreSevere(worst, h);
        worst = moreSevere(worst, r);
        worst = moreSevere(worst, sys);
        worst = moreSevere(worst, dia);

        return worst;
    }

    private AlertLevel moreSevere(AlertLevel a, AlertLevel b) {
        if (a == AlertLevel.URGENT || b == AlertLevel.URGENT) return AlertLevel.URGENT;
        if (a == AlertLevel.WARNING || b == AlertLevel.WARNING) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }
}
