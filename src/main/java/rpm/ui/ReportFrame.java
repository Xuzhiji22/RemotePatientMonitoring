package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.dao.AbnormalEventDao;
import rpm.dao.MinuteAverageDao;
import rpm.data.AbnormalEvent;
import rpm.data.MinuteRecord;
import rpm.data.PatientHistoryStore;
import rpm.model.Patient;
import rpm.client.CloudReportClient;
import rpm.config.ConfigStore;
import java.nio.file.Paths;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportFrame extends JFrame {

    private final Patient patient;
    @SuppressWarnings("unused")
    private final PatientHistoryStore history; // 保留：你们本来就传进来，后续也可做“本地历史对照”
    @SuppressWarnings("unused")
    private final AlertEngine alertEngine;
    private final JFrame back;

    private final JTextArea textArea = new JTextArea();
    private JButton btnGen;
    private boolean loading = false;


    public ReportFrame(Patient patient, PatientHistoryStore history, AlertEngine alertEngine, JFrame back) {
        this.patient = patient;
        this.history = history;
        this.alertEngine = alertEngine;
        this.back = back;

        setTitle("Daily Report - " + patient.patientId());
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        buildUi();
        generateLast24hReport();
    }

    private void buildUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        btnGen = new JButton("Generate (Last 24h)");
        JButton btnCopy = new JButton("Copy");
        JButton btnBack = new JButton("Back");

        btnGen.addActionListener(e -> generateLast24hReport());
        btnCopy.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
        });
        btnBack.addActionListener(e -> {
            dispose();
            if (back != null) back.setVisible(true);
        });

        top.add(btnGen);
        top.add(btnCopy);
        top.add(btnBack);

        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(textArea);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }

    private void generateLast24hReport() {
        if (loading) return;
        loading = true;

        long now = System.currentTimeMillis();
        long from = now - 24L * 60L * 60L * 1000L;

        // Read cloud baseUrl + timeout from system.properties
        ConfigStore cfg = new ConfigStore(Paths.get("data", "system.properties"));
        final String cloudBaseUrl = cfg.getString("cloud.baseUrl", "https://bioeng-rpm-app.impaas.uk");
        final int cloudLimit = cfg.getInt("cloud.report.limit", 2000);

        // UI state
        if (btnGen != null) btnGen.setEnabled(false);
        textArea.setText("Generating report...\n");
        textArea.setCaretPosition(0);

        SwingWorker<String, Void> worker = new SwingWorker<>() {

            @Override
            protected String doInBackground() {
                // ===== 1) Cloud-first =====
                try {
                    CloudReportClient client = new CloudReportClient(cloudBaseUrl);

                    String minutesJson = client.getLatestMinutesJson(patient.patientId(), cloudLimit);
                    String abnormalJson = client.getLatestAbnormalJson(patient.patientId(), cloudLimit);

                    return buildCloudTextReport(patient, from, now, minutesJson, abnormalJson);

                } catch (Exception cloudEx) {
                    System.err.println("[ReportFrame] cloud report failed: " + cloudEx.getMessage());

                    // ===== 2) Fallback: local DB =====
                    try {
                        MinuteAverageDao minuteDao = new MinuteAverageDao();
                        AbnormalEventDao abnormalDao = new AbnormalEventDao();

                        List<MinuteRecord> minutes = minuteDao.latest(patient.patientId(), 2000);
                        List<AbnormalEvent> abns = abnormalDao.latest(patient.patientId(), 2000);

                        return buildTextReport(patient, from, now, minutes, abns)
                                + "\n\n[Note] Cloud report failed, showing local DB report fallback.\n"
                                + "Cloud error: " + cloudEx.getMessage() + "\n";

                    } catch (Exception dbEx) {
                        return "Failed to generate report.\n"
                                + "Cloud error: " + cloudEx.getMessage() + "\n"
                                + "Local DB error: " + dbEx.getMessage() + "\n";
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    String report = get();
                    textArea.setText(report);
                    textArea.setCaretPosition(0);
                } catch (Exception e) {
                    textArea.setText("Failed to generate report: " + e.getMessage());
                } finally {
                    loading = false;
                    if (btnGen != null) btnGen.setEnabled(true);
                }
            }
        };

        worker.execute();
    }



    private String buildTextReport(
            Patient p,
            long fromMs,
            long toMs,
            List<MinuteRecord> minutes,
            List<AbnormalEvent> abns
    ) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder();

        sb.append("===== Daily Report (Last 24h) =====\n");
        sb.append("Patient: ").append(p.patientId()).append(" / ").append(p.name()).append("\n");
        sb.append("Ward: ").append(p.ward()).append("\n");
        sb.append("Window: ").append(fmt.format(Instant.ofEpochMilli(fromMs)))
                .append("  ->  ")
                .append(fmt.format(Instant.ofEpochMilli(toMs))).append("\n\n");

        // ---- Minute averages summary (filter by time window)
        double sumT = 0, sumHR = 0, sumRR = 0, sumSys = 0, sumDia = 0;
        int count = 0;

        for (MinuteRecord m : minutes) {
            long t = m.minuteStartMs();
            if (t < fromMs || t > toMs) continue;
            sumT += m.avgTemp();
            sumHR += m.avgHR();
            sumRR += m.avgRR();
            sumSys += m.avgSys();
            sumDia += m.avgDia();
            count++;
        }

        sb.append("---- Minute Averages ----\n");
        if (count == 0) {
            sb.append("No minute averages in this window.\n\n");
        } else {
            sb.append("Minutes recorded: ").append(count).append("\n");
            sb.append(String.format("Avg Temp: %.2f\n", sumT / count));
            sb.append(String.format("Avg HR  : %.2f\n", sumHR / count));
            sb.append(String.format("Avg RR  : %.2f\n", sumRR / count));
            sb.append(String.format("Avg SYS : %.2f\n", sumSys / count));
            sb.append(String.format("Avg DIA : %.2f\n\n", sumDia / count));
        }

        // ---- Abnormal events list (filter by time window)
        sb.append("---- Abnormal Instances ----\n");
        int abCount = 0;
        for (AbnormalEvent a : abns) {
            long ts = a.timestampMs();
            if (ts < fromMs || ts > toMs) continue;
            abCount++;
            sb.append(fmt.format(Instant.ofEpochMilli(ts)))
                    .append(" | ").append(a.level())
                    .append(" | ").append(a.vitalType())
                    .append(" | ").append(a.value())
                    .append(" | ").append(a.message())
                    .append("\n");
        }

        if (abCount == 0) {
            sb.append("No abnormal instances in this window.\n");
        }

        sb.append("\n===== End =====\n");
        return sb.toString();
    }

    private String buildCloudTextReport(Patient p, long fromMs, long toMs,
                                        String minutesJson, String abnormalJson) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder();
        sb.append("===== Daily Report (Last 24h) [CLOUD] =====\n");
        sb.append("Patient: ").append(p.patientId()).append(" / ").append(p.name()).append("\n");
        sb.append("Ward: ").append(p.ward()).append("\n");
        sb.append("Window: ").append(fmt.format(Instant.ofEpochMilli(fromMs)))
                .append("  ->  ")
                .append(fmt.format(Instant.ofEpochMilli(toMs))).append("\n\n");

        sb.append("---- Minute Averages (JSON from /api/minutes/latest) ----\n");
        sb.append(minutesJson).append("\n\n");

        sb.append("---- Abnormal Instances (JSON from /api/abnormal/latest) ----\n");
        sb.append(abnormalJson).append("\n");

        sb.append("\n===== End =====\n");
        return sb.toString();
    }
}
