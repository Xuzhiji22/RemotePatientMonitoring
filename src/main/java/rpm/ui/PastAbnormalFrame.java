package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.AbnormalEvent;
import rpm.data.PatientHistoryStore;
import rpm.model.AlertLevel;
import rpm.model.Patient;
import rpm.model.VitalType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PastAbnormalFrame extends JFrame {

    private final Patient patient;
    private final PatientHistoryStore history;
    private final AlertEngine alertEngine;
    private final JFrame parent;

    private final DefaultTableModel model;
    private final JTable table;

    private final JComboBox<String> rangeCombo;
    private final JComboBox<VitalType> typeCombo;
    private final JCheckBox urgentOnly;
    private final JLabel summaryLabel;

    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PastAbnormalFrame(Patient patient, PatientHistoryStore history, AlertEngine alertEngine, JFrame parent) {
        super("Past Abnormal - " + patient.patientId());
        this.patient = patient;
        this.history = history;
        this.alertEngine = alertEngine;
        this.parent = parent;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        JButton back = new JButton("Back");
        back.addActionListener(e -> goBack());
        top.add(back, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filters.add(new JLabel("Range:"));
        rangeCombo = new JComboBox<>(new String[]{"Last 24 Hours", "Last 72 Hours", "Last 7 Days"});
        filters.add(rangeCombo);

        filters.add(new JLabel("Vital:"));
        typeCombo = new JComboBox<>();
        typeCombo.addItem(null); // null 表示 ALL
        typeCombo.addItem(VitalType.BODY_TEMPERATURE);
        typeCombo.addItem(VitalType.HEART_RATE);
        typeCombo.addItem(VitalType.RESPIRATORY_RATE);
        typeCombo.addItem(VitalType.SYSTOLIC_BP);
        typeCombo.addItem(VitalType.DIASTOLIC_BP);
        filters.add(typeCombo);

        urgentOnly = new JCheckBox("Urgent only");
        filters.add(urgentOnly);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());
        filters.add(refresh);

        top.add(filters, BorderLayout.CENTER);

        // Table
        model = new DefaultTableModel(new Object[]{
                "Time", "Vital", "Value", "Level", "Warning Range", "Urgent Range", "Message"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(3).setCellRenderer(new LevelRenderer());

        JScrollPane scroll = new JScrollPane(table);

        // Bottom summary
        JPanel bottom = new JPanel(new BorderLayout());
        summaryLabel = new JLabel(" ");
        summaryLabel.setForeground(Color.GRAY);
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bottom.add(summaryLabel, BorderLayout.WEST);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        rangeCombo.addActionListener(e -> reload());
        typeCombo.addActionListener(e -> reload());
        urgentOnly.addActionListener(e -> reload());

        rangeCombo.setSelectedIndex(0); // default 24h
        reload();
    }

    private void goBack() {
        this.dispose();
        if (parent != null) parent.setVisible(true);
    }

    private void reload() {
        model.setRowCount(0);

        // 1) choose range
        List<AbnormalEvent> events;
        int ridx = rangeCombo.getSelectedIndex();
        if (ridx == 0) {
            events = history.getAbnormalLastHours(24);
        } else if (ridx == 1) {
            events = history.getAbnormalLastHours(72);
        } else {
            events = history.getAbnormalLastDays(7);
        }

        if (events == null || events.isEmpty()) {
            summaryLabel.setText("No abnormal events in this range yet. (Run simulation until warnings/urgents appear.)");
            return;
        }

        VitalType filterType = (VitalType) typeCombo.getSelectedItem();
        boolean onlyUrgent = urgentOnly.isSelected();

        int urgentCount = 0;
        int warningCount = 0;

        // events already newest-first if your history reversed them; even if not, it's ok.
        for (AbnormalEvent e : events) {
            if (e == null) continue;

            VitalType vt = e.vitalType();
            if (filterType != null && vt != filterType) continue;

            AlertLevel level = e.level();
            if (level == null) continue;
            if (onlyUrgent && level != AlertLevel.URGENT) continue;

            if (level == AlertLevel.URGENT) urgentCount++;
            else if (level == AlertLevel.WARNING) warningCount++;

            var th = alertEngine.getThreshold(vt);
            String warnRange   = th == null ? "-" : (th.warnLow() + " ~ " + th.warnHigh());
            String urgentRange = th == null ? "-" : ("<" + th.urgentLow() + " or >" + th.urgentHigh());

            model.addRow(new Object[]{
                    fmt.format(new Date(e.timestampMs())),
                    pretty(vt),
                    formatValue(vt, e.value()),
                    level.toString(),
                    warnRange,
                    urgentRange,
                    e.message() == null ? "" : e.message()
            });
        }

        summaryLabel.setText(String.format(
                "Patient %s | Rows: %d (URGENT: %d, WARNING: %d) | Source: %d real abnormal-events",
                patient.patientId(), model.getRowCount(), urgentCount, warningCount, events.size()
        ));
    }

    private String pretty(VitalType t) {
        if (t == null) return "-";
        switch (t) {
            case BODY_TEMPERATURE:
                return "Body Temp";
            case HEART_RATE:
                return "Heart Rate";
            case RESPIRATORY_RATE:
                return "Resp Rate";
            case SYSTOLIC_BP:
                return "Systolic BP";
            case DIASTOLIC_BP:
                return "Diastolic BP";
            default:
                return t.toString();
        }
    }

    private String formatValue(VitalType t, double v) {
        if (t == null) return String.format("%.2f", v);
        switch (t) {
            case BODY_TEMPERATURE:
                return String.format("%.2f °C", v);
            case HEART_RATE:
                return String.format("%.0f bpm", v);
            case RESPIRATORY_RATE:
                return String.format("%.0f rpm", v);
            case SYSTOLIC_BP:
            case DIASTOLIC_BP:
                return String.format("%.0f mmHg", v);
            default:
                return String.format("%.2f", v);
        }
    }

    private static class LevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String s = value == null ? "" : value.toString();

            if (!isSelected) {
                if ("URGENT".equals(s)) c.setForeground(new Color(180, 0, 0));
                else if ("WARNING".equals(s)) c.setForeground(new Color(180, 120, 0));
                else c.setForeground(new Color(0, 140, 0));
            }
            return c;
        }
    }
}
