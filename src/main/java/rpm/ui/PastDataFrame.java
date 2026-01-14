package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.MinuteRecord;
import rpm.data.PatientHistoryStore;
import rpm.model.Patient;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PastDataFrame extends JFrame {

    private final Patient patient;
    private final PatientHistoryStore history;
    private final AlertEngine alertEngine;

    private final PastVitalChartPanel chart;
    private final JLabel selectedInfo = new JLabel("Select a minute…");

    private List<MinuteRecord> records = List.of();

    private final JComboBox<String> rangeBox = new JComboBox<>(new String[]{
            "Last 24 Hours", "Last 72 Hours", "Last 7 Days"
    });

    private final JComboBox<String> vitalBox = new JComboBox<>(new String[]{
            "Body temperature", "Heart rate", "Respiratory rate", "Systolic BP", "Diastolic BP"
    });

    private final JSlider minuteSlider = new JSlider(0, 0, 0);

    public PastDataFrame(Patient patient, PatientHistoryStore history, AlertEngine alertEngine, JFrame parent) {
        super("Past Vital Data - " + patient.patientId());
        this.patient = patient;
        this.history = history;
        this.alertEngine = alertEngine;

        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        chart = new PastVitalChartPanel(alertEngine);

        JButton back = new JButton("Back");
        back.addActionListener(e -> {
            this.dispose();
            parent.setVisible(true);
        });

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        top.add(back);

        top.add(new JLabel("Range:"));
        top.add(rangeBox);

        top.add(new JLabel("Vital:"));
        top.add(vitalBox);

        top.add(refresh);

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottom.add(new JLabel("Select minute (0 = oldest, rightmost = latest):"), BorderLayout.NORTH);
        bottom.add(minuteSlider, BorderLayout.CENTER);
        bottom.add(selectedInfo, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(chart, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        // listeners
        rangeBox.addActionListener(e -> reload());
        vitalBox.addActionListener(e -> applyVitalSelection());
        minuteSlider.addChangeListener(e -> applyMinuteSelection());

        // default: last 24h
        rangeBox.setSelectedIndex(0);

        // load data
        reload();
    }

    private void reload() {
        int idx = rangeBox.getSelectedIndex();
        if (idx == 0) {
            records = history.getLastHours(24);
            setTitle("Past Vital Data (24h) - " + patient.patientId());
        } else if (idx == 1) {
            records = history.getLastHours(72);
            setTitle("Past Vital Data (72h) - " + patient.patientId());
        } else {
            records = history.getLastDays(7);
            setTitle("Past Vital Data (7d) - " + patient.patientId());
        }

        if (records.isEmpty()) {
            selectedInfo.setText("No minute records in this range yet.");
            minuteSlider.setMinimum(0);
            minuteSlider.setMaximum(0);
            minuteSlider.setValue(0);
            chart.setData(List.of());
            return;
        }

        chart.setData(records);

        minuteSlider.setMinimum(0);
        minuteSlider.setMaximum(records.size() - 1);
        minuteSlider.setValue(records.size() - 1); // default latest

        applyVitalSelection();
        applyMinuteSelection();
    }

    private void applyVitalSelection() {
        int idx = vitalBox.getSelectedIndex();
        switch (idx) {
            case 0:
                chart.setVital(VitalType.BODY_TEMPERATURE, MinuteRecord::avgTemp);
                break;
            case 1:
                chart.setVital(VitalType.HEART_RATE, MinuteRecord::avgHR);
                break;
            case 2:
                chart.setVital(VitalType.RESPIRATORY_RATE, MinuteRecord::avgRR);
                break;
            case 3:
                chart.setVital(VitalType.SYSTOLIC_BP, MinuteRecord::avgSys);
                break;
            case 4:
                chart.setVital(VitalType.DIASTOLIC_BP, MinuteRecord::avgDia);
                break;
            default:
                chart.setVital(VitalType.HEART_RATE, MinuteRecord::avgHR);
                break;
        }
        chart.repaint();
    }

    private void applyMinuteSelection() {
        if (records.isEmpty()) return;

        int i = minuteSlider.getValue();
        if (i < 0 || i >= records.size()) return;

        chart.setSelectedIndex(i);

        MinuteRecord r = records.get(i);
        String time = Instant.ofEpochMilli(r.minuteStartMs())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        selectedInfo.setText(String.format(
                "Minute: %s | Temp=%.2f°C, HR=%.1f bpm, RR=%.1f rpm, Sys=%.1f, Dia=%.1f (n=%d)",
                time, r.avgTemp(), r.avgHR(), r.avgRR(), r.avgSys(), r.avgDia(), r.sampleCount()
        ));
    }
}
