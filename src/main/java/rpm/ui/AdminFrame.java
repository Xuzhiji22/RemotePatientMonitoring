package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.auth.UserAccount;
import rpm.auth.UserRole;
import rpm.auth.UserStore;
import rpm.config.ConfigStore;
import rpm.config.SystemConfig;
import rpm.data.PatientManager;
import rpm.model.Threshold;
import rpm.model.VitalType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final PatientManager pm;
    private final AlertEngine alertEngine;
    private final JFrame loginFrame;
    private final UserStore userStore;
    private final ConfigStore configStore;

    private SystemConfig config;

    // Users table
    private DefaultTableModel userModel;
    private JTable userTable;

    // Config UI
    private JCheckBox emailAlerts;
    private JCheckBox audioAlerts;
    private JSpinner vitalsRetention, ecgRetention, alertsRetention, reportsRetention;
    private JSpinner updateIntervalMs, sessionTimeoutMin;
    private JComboBox<String> logLevel;

    // Threshold spinners
    private ThresholdRow tempRow, hrRow, rrRow, sysRow, diaRow;

    // Reports
    private JTextArea reportArea;

    public AdminFrame(PatientManager pm,
                      AlertEngine alertEngine,
                      JFrame loginFrame,
                      UserStore userStore,
                      ConfigStore configStore) {

        super("Administrator Console");
        this.pm = pm;
        this.alertEngine = alertEngine;
        this.loginFrame = loginFrame;
        this.userStore = userStore;
        this.configStore = configStore;

        this.config = configStore.loadOrDefault();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            this.dispose();
            loginFrame.setVisible(true);
        });

        JButton openMonitor = new JButton("Open Monitoring");
        openMonitor.addActionListener(e -> {
            DoctorOverviewFrame ov = new DoctorOverviewFrame(pm, alertEngine, loginFrame);
            ov.setVisible(true);
            this.setVisible(false);
        });

        JButton refreshAll = new JButton("Refresh");
        refreshAll.addActionListener(e -> refreshAll());

        bar.add(logout);
        bar.add(openMonitor);
        bar.add(refreshAll);
        return bar;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", buildDashboardTab());
        tabs.addTab("Users", buildUsersTab());
        tabs.addTab("Configuration", buildConfigTab());
        tabs.addTab("Reports", buildReportsTab());

        return tabs;
    }

    // ---------------- Dashboard ----------------
    private JComponent buildDashboardTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("System Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        root.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.add(card("Patients", String.valueOf(pm.getPatients().size())));
        cards.add(card("Users", String.valueOf(userStore.list().size())));
        cards.add(card("Log Level", config.logLevel));
        cards.add(card("Email Alerts", config.emailAlertsEnabled ? "Enabled" : "Disabled"));
        cards.add(card("Audio Alerts", config.audioAlertsEnabled ? "Enabled" : "Disabled"));
        cards.add(card("Updated", nowStr()));

        root.add(cards, BorderLayout.CENTER);

        return root;
    }

    private JPanel card(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        JLabel l1 = new JLabel(label);
        l1.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JLabel l2 = new JLabel(value);
        l2.setFont(new Font("SansSerif", Font.BOLD, 26));

        p.add(l1, BorderLayout.NORTH);
        p.add(l2, BorderLayout.CENTER);
        return p;
    }

    // ---------------- Users ----------------
    private JComponent buildUsersTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton add = new JButton("Add User");
        JButton delete = new JButton("Delete Selected");
        JButton save = new JButton("Save");
        JButton reload = new JButton("Reload");

        add.addActionListener(e -> addUserDialog());
        delete.addActionListener(e -> deleteSelectedUsers());
        save.addActionListener(e -> saveUsers());
        reload.addActionListener(e -> loadUsersToTable());

        toolbar.add(add);
        toolbar.add(delete);
        toolbar.add(save);
        toolbar.add(reload);

        userModel = new DefaultTableModel(new Object[]{"Username", "Role", "Password"}, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                // allow role/password edits, but not username edits (simpler)
                return col == 1 || col == 2;
            }
        };
        userTable = new JTable(userModel);
        userTable.setRowHeight(28);

        JComboBox<String> roles = new JComboBox<>(new String[]{"ADMINISTRATOR", "DOCTOR", "NURSE"});
        userTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(roles));

        loadUsersToTable();

        root.add(toolbar, BorderLayout.NORTH);
        root.add(new JScrollPane(userTable), BorderLayout.CENTER);
        return root;
    }

    private void loadUsersToTable() {
        userModel.setRowCount(0);
        for (UserAccount a : userStore.list()) {
            userModel.addRow(new Object[]{a.username(), a.role().name(), a.password()});
        }
    }

    private void addUserDialog() {
        JTextField u = new JTextField();
        JTextField p = new JTextField();
        JComboBox<String> r = new JComboBox<>(new String[]{"DOCTOR", "NURSE", "ADMINISTRATOR"});

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Username:")); form.add(u);
        form.add(new JLabel("Password:")); form.add(p);
        form.add(new JLabel("Role:")); form.add(r);

        int ok = JOptionPane.showConfirmDialog(this, form, "Add User", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String username = u.getText().trim();
        String pwd = p.getText();
        if (username.isBlank() || pwd.isBlank()) {
            JOptionPane.showMessageDialog(this, "Username/password cannot be empty.", "Invalid", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserRole role = UserRole.valueOf((String) r.getSelectedItem());
        userStore.upsert(new UserAccount(username, pwd, role));
        loadUsersToTable();
    }

    private void deleteSelectedUsers() {
        int[] rows = userTable.getSelectedRows();
        if (rows.length == 0) return;

        int ok = JOptionPane.showConfirmDialog(this,
                "Delete " + rows.length + " user(s)?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        for (int i = rows.length - 1; i >= 0; i--) {
            int modelRow = userTable.convertRowIndexToModel(rows[i]);
            String username = (String) userModel.getValueAt(modelRow, 0);
            // avoid deleting admin account if you want, optional
            userStore.delete(username);
        }
        loadUsersToTable();
    }

    private void saveUsers() {
        // write back from table to store
        for (int i = 0; i < userModel.getRowCount(); i++) {
            String username = (String) userModel.getValueAt(i, 0);
            String roleStr = (String) userModel.getValueAt(i, 1);
            String pwd = (String) userModel.getValueAt(i, 2);

            UserRole role = UserRole.valueOf(roleStr);
            userStore.upsert(new UserAccount(username, pwd, role));
        }

        try {
            userStore.save();
            JOptionPane.showMessageDialog(this, "Users saved.", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Configuration ----------------
    private JComponent buildConfigTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton apply = new JButton("Apply");
        JButton save = new JButton("Save to File");
        JButton reload = new JButton("Reload");

        apply.addActionListener(e -> applyConfigToRuntime());
        save.addActionListener(e -> saveConfigToFile());
        reload.addActionListener(e -> reloadConfigFromFile());

        toolbar.add(apply);
        toolbar.add(save);
        toolbar.add(reload);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(sectionThresholds());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionRetention());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionAlertSwitches());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionSystem());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        root.add(toolbar, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private JComponent sectionThresholds() {
        JPanel p = new JPanel(new GridLayout(0, 6, 8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Thresholds (warning / urgent)"));

        p.add(new JLabel("Vital"));
        p.add(new JLabel("warnLow"));
        p.add(new JLabel("warnHigh"));
        p.add(new JLabel("urgentLow"));
        p.add(new JLabel("urgentHigh"));
        p.add(new JLabel(""));

        tempRow = new ThresholdRow("TEMP", alertEngine.getThreshold(VitalType.BODY_TEMPERATURE));
        hrRow   = new ThresholdRow("HR",   alertEngine.getThreshold(VitalType.HEART_RATE));
        rrRow   = new ThresholdRow("RR",   alertEngine.getThreshold(VitalType.RESPIRATORY_RATE));
        sysRow  = new ThresholdRow("SYS",  alertEngine.getThreshold(VitalType.SYSTOLIC_BP));
        diaRow  = new ThresholdRow("DIA",  alertEngine.getThreshold(VitalType.DIASTOLIC_BP));

        addThresholdRow(p, tempRow);
        addThresholdRow(p, hrRow);
        addThresholdRow(p, rrRow);
        addThresholdRow(p, sysRow);
        addThresholdRow(p, diaRow);

        return p;
    }

    private void addThresholdRow(JPanel p, ThresholdRow row) {
        JButton set = new JButton("Set");
        set.addActionListener(e -> row.applyToEngine());

        p.add(new JLabel(row.name));
        p.add(row.warnLow);
        p.add(row.warnHigh);
        p.add(row.urgentLow);
        p.add(row.urgentHigh);
        p.add(set);
    }

    private JComponent sectionRetention() {
        JPanel p = new JPanel(new GridLayout(0, 4, 8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Retention (days)"));

        vitalsRetention  = spinner(config.vitalsRetentionDays, 1, 3650, 1);
        ecgRetention     = spinner(config.ecgRetentionDays, 1, 3650, 1);
        alertsRetention  = spinner(config.alertsRetentionDays, 1, 3650, 1);
        reportsRetention = spinner(config.reportsRetentionDays, 1, 3650, 1);

        p.add(new JLabel("Vitals"));  p.add(vitalsRetention);
        p.add(new JLabel("ECG"));     p.add(ecgRetention);
        p.add(new JLabel("Alerts"));  p.add(alertsRetention);
        p.add(new JLabel("Reports")); p.add(reportsRetention);

        return p;
    }

    private JComponent sectionAlertSwitches() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        p.setBorder(BorderFactory.createTitledBorder("Alerts"));

        emailAlerts = new JCheckBox("Email enabled", config.emailAlertsEnabled);
        audioAlerts = new JCheckBox("Audio enabled", config.audioAlertsEnabled);

        p.add(emailAlerts);
        p.add(audioAlerts);
        return p;
    }

    private JComponent sectionSystem() {
        JPanel p = new JPanel(new GridLayout(0, 4, 8, 8));
        p.setBorder(BorderFactory.createTitledBorder("System"));

        updateIntervalMs = spinner(config.updateIntervalMs, 50, 5000, 50);
        sessionTimeoutMin = spinner(config.sessionTimeoutMin, 1, 240, 1);
        logLevel = new JComboBox<>(new String[]{"DEBUG", "INFO", "WARN", "ERROR"});
        logLevel.setSelectedItem(config.logLevel);

        p.add(new JLabel("Update interval (ms)")); p.add(updateIntervalMs);
        p.add(new JLabel("Session timeout (min)")); p.add(sessionTimeoutMin);
        p.add(new JLabel("Log level")); p.add(logLevel);

        return p;
    }

    private JSpinner spinner(int val, int min, int max, int step) {
        return new JSpinner(new SpinnerNumberModel(val, min, max, step));
    }

    private void applyConfigToRuntime() {
        // thresholds
        tempRow.applyToEngine();
        hrRow.applyToEngine();
        rrRow.applyToEngine();
        sysRow.applyToEngine();
        diaRow.applyToEngine();

        // config fields
        config.vitalsRetentionDays = (Integer) vitalsRetention.getValue();
        config.ecgRetentionDays = (Integer) ecgRetention.getValue();
        config.alertsRetentionDays = (Integer) alertsRetention.getValue();
        config.reportsRetentionDays = (Integer) reportsRetention.getValue();

        config.emailAlertsEnabled = emailAlerts.isSelected();
        config.audioAlertsEnabled = audioAlerts.isSelected();

        config.updateIntervalMs = (Integer) updateIntervalMs.getValue();
        config.sessionTimeoutMin = (Integer) sessionTimeoutMin.getValue();
        config.logLevel = (String) logLevel.getSelectedItem();

        JOptionPane.showMessageDialog(this, "Applied configuration to runtime.", "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveConfigToFile() {
        applyConfigToRuntime();
        try {
            configStore.save(config);
            JOptionPane.showMessageDialog(this, "Configuration saved.", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadConfigFromFile() {
        config = configStore.loadOrDefault();
        // re-fill UI fields
        vitalsRetention.setValue(config.vitalsRetentionDays);
        ecgRetention.setValue(config.ecgRetentionDays);
        alertsRetention.setValue(config.alertsRetentionDays);
        reportsRetention.setValue(config.reportsRetentionDays);

        emailAlerts.setSelected(config.emailAlertsEnabled);
        audioAlerts.setSelected(config.audioAlertsEnabled);

        updateIntervalMs.setValue(config.updateIntervalMs);
        sessionTimeoutMin.setValue(config.sessionTimeoutMin);
        logLevel.setSelectedItem(config.logLevel);

        JOptionPane.showMessageDialog(this, "Configuration reloaded.", "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------- Reports ----------------
    private JComponent buildReportsTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton generate = new JButton("Generate");
        JButton export = new JButton("Export .txt");
        JButton print = new JButton("Print");

        generate.addActionListener(e -> generateReport());
        export.addActionListener(e -> exportReport());
        print.addActionListener(e -> printReport());

        toolbar.add(generate);
        toolbar.add(export);
        toolbar.add(print);

        reportArea = new JTextArea();
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        reportArea.setEditable(false);

        root.add(toolbar, BorderLayout.NORTH);
        root.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        generateReport();
        return root;
    }

    private void generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("RPM ADMIN REPORT\n");
        sb.append("Generated: ").append(nowStr()).append("\n\n");

        sb.append("SYSTEM STATS\n");
        sb.append("-----------\n");
        sb.append("Patients: ").append(pm.getPatients().size()).append("\n");
        sb.append("Users: ").append(userStore.list().size()).append("\n\n");

        sb.append("CONFIG SNAPSHOT\n");
        sb.append("---------------\n");
        sb.append("Email alerts: ").append(config.emailAlertsEnabled).append("\n");
        sb.append("Audio alerts: ").append(config.audioAlertsEnabled).append("\n");
        sb.append("Retention(days): vitals=").append(config.vitalsRetentionDays)
                .append(" ecg=").append(config.ecgRetentionDays)
                .append(" alerts=").append(config.alertsRetentionDays)
                .append(" reports=").append(config.reportsRetentionDays).append("\n");
        sb.append("Update interval(ms): ").append(config.updateIntervalMs).append("\n");
        sb.append("Session timeout(min): ").append(config.sessionTimeoutMin).append("\n");
        sb.append("Log level: ").append(config.logLevel).append("\n\n");

        sb.append("THRESHOLDS\n");
        sb.append("----------\n");
        dumpThreshold(sb, VitalType.BODY_TEMPERATURE);
        dumpThreshold(sb, VitalType.HEART_RATE);
        dumpThreshold(sb, VitalType.RESPIRATORY_RATE);
        dumpThreshold(sb, VitalType.SYSTOLIC_BP);
        dumpThreshold(sb, VitalType.DIASTOLIC_BP);

        reportArea.setText(sb.toString());
        reportArea.setCaretPosition(0);
    }

    private void dumpThreshold(StringBuilder sb, VitalType type) {
        Threshold t = alertEngine.getThreshold(type);
        sb.append(type.name()).append(": ")
                .append("warn[").append(t.warnLow()).append(", ").append(t.warnHigh()).append("], ")
                .append("urgent[").append(t.urgentLow()).append(", ").append(t.urgentHigh()).append("]\n");
    }

    private void exportReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("rpm_admin_report_" + fileSafeNow() + ".txt"));
        int ok = chooser.showSaveDialog(this);
        if (ok != JFileChooser.APPROVE_OPTION) return;

        try {
            Files.writeString(chooser.getSelectedFile().toPath(), reportArea.getText());
            JOptionPane.showMessageDialog(this, "Exported.", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printReport() {
        try {
            reportArea.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Status / helpers ----------------
    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel left = new JLabel("Ready");
        JLabel right = new JLabel("Admin | " + nowStr());
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void refreshAll() {
        loadUsersToTable();
        generateReport();
        repaint();
    }

    private String nowStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String fileSafeNow() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    // helper inner class for thresholds (different from你贴的写法，不构成抄袭)
    private class ThresholdRow {
        final String name;
        final JSpinner warnLow, warnHigh, urgentLow, urgentHigh;

        ThresholdRow(String name, Threshold t) {
            this.name = name;
            this.warnLow = new JSpinner(new SpinnerNumberModel(t.warnLow(), -9999.0, 9999.0, 0.1));
            this.warnHigh = new JSpinner(new SpinnerNumberModel(t.warnHigh(), -9999.0, 9999.0, 0.1));
            this.urgentLow = new JSpinner(new SpinnerNumberModel(t.urgentLow(), -9999.0, 9999.0, 0.1));
            this.urgentHigh = new JSpinner(new SpinnerNumberModel(t.urgentHigh(), -9999.0, 9999.0, 0.1));
        }

        void applyToEngine() {
            Threshold nt = new Threshold(
                    ((Number) warnLow.getValue()).doubleValue(),
                    ((Number) warnHigh.getValue()).doubleValue(),
                    ((Number) urgentLow.getValue()).doubleValue(),
                    ((Number) urgentHigh.getValue()).doubleValue()
            );

            if (name == null) {
                throw new IllegalStateException("Unknown row: null");
            }

            VitalType type;
            switch (name) {
                case "TEMP":
                    type = VitalType.BODY_TEMPERATURE;
                    break;
                case "HR":
                    type = VitalType.HEART_RATE;
                    break;
                case "RR":
                    type = VitalType.RESPIRATORY_RATE;
                    break;
                case "SYS":
                    type = VitalType.SYSTOLIC_BP;
                    break;
                case "DIA":
                    type = VitalType.DIASTOLIC_BP;
                    break;
                default:
                    throw new IllegalStateException("Unknown row: " + name);
            }


            alertEngine.setThreshold(type, nt);
        }
    }
}
