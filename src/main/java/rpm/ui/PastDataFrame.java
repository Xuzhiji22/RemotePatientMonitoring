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
import java.util.function.ToDoubleFunction;

public class PastDataFrame extends JFrame {

    private final Patient patient;
    private final PatientHistoryStore history;
    private final AlertEngine alertEngine;

    private final PastVitalChartPanel chart;
    private final JLabel selectedInfo = new JLabel("Select a minute…");

    private List<MinuteRecord> records = List.of();

    private final JComboBox<String> vitalBox = new JComboBox<>(new String[]{
            "Body temperature", "Heart rate", "Respiratory rate", "Systolic BP", "Diastolic BP"
    });

    private final JSlider minuteSlider = new JSlider(0, 0, 0);

    public PastDataFrame(Patient patient, PatientHistoryStore history, AlertEngine alertEngine, JFrame parent) {
        super("Past Vital Data (24h) - " + patient.patientId());
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

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        top.add(back);
        top.add(new JLabel("Vital:"));
        top.add(vitalBox);

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
        vitalBox.addActionListener(e -> applyVitalSelection());
        minuteSlider.addChangeListener(e -> applyMinuteSelection());

        // load data
        reload();
    }

    private void reload() {
        records = history.getLast24Hours();

        if (records.isEmpty()) {
            selectedInfo.setText("No minute records yet. Wait a bit (minute averages appear after 1 minute).");
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
            case 0 -> chart.setVital(VitalType.BODY_TEMPERATURE, MinuteRecord::avgTemp);
            case 1 -> chart.setVital(VitalType.HEART_RATE, MinuteRecord::avgHR);
            case 2 -> chart.setVital(VitalType.RESPIRATORY_RATE, MinuteRecord::avgRR);
            case 3 -> chart.setVital(VitalType.SYSTOLIC_BP, MinuteRecord::avgSys);
            case 4 -> chart.setVital(VitalType.DIASTOLIC_BP, MinuteRecord::avgDia);
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

        // display all vitals for that minute (你说“可以选择每分钟” -> 选中分钟就展示该分钟的数据)
        selectedInfo.setText(String.format(
                "Minute: %s | Temp=%.2f°C, HR=%.1f bpm, RR=%.1f rpm, Sys=%.1f, Dia=%.1f (n=%d)",
                time, r.avgTemp(), r.avgHR(), r.avgRR(), r.avgSys(), r.avgDia(), r.sampleCount()
        ));
    }
}

