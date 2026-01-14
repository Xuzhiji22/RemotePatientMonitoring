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
import rpm.model.AlertLevel;

import java.nio.file.Paths;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class ReportFrame extends JFrame {


    private static final long serialVersionUID = 1L;
    private final Patient patient;
    private final JFrame back;

    private final JTextArea textArea = new JTextArea();
    private JButton btnGen;
    private boolean loading = false;

    private static final Gson GSON = new Gson();
    private static final int MAX_ABNORMAL_LINES = 10;
    private static final boolean REPORT_DEBUG_RAW_JSON = false;


    public ReportFrame(Patient patient, PatientHistoryStore history, AlertEngine alertEngine, JFrame back) {
        this.patient = Objects.requireNonNull(patient);
        Objects.requireNonNull(history); // kept for API compatibility / future use
        Objects.requireNonNull(alertEngine); // kept for API compatibility / future use
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
        final int cloudLimit = cfg.getInt("cloud.report.limit", 20000);

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

        // -------- Parse minutes JSON -> summary stats --------
        SummaryStats stats = parseMinutesSummary(minutesJson, fromMs, toMs);

        sb.append("--- Summary ---\n");
        if (stats.total == 0) {
            sb.append("No minute averages in this window.\n\n");
        } else {

            int expected = (int) Math.max(1, (toMs - fromMs) / (60_000L));
            double coverage = 100.0 * stats.total / expected;

            sb.append(String.format("Data coverage: %d / %d minutes (%.1f%%)\n",
                    stats.total, expected, coverage));

            sb.append(String.format("Temp (°C): mean %.2f | min %.2f | max %.2f\n",
                    stats.meanTemp(), stats.minTemp, stats.maxTemp));
            sb.append(String.format("HR (bpm):  mean %.2f | min %.2f | max %.2f\n",
                    stats.meanHr(), stats.minHr, stats.maxHr));
            sb.append(String.format("RR (/min): mean %.2f | min %.2f | max %.2f\n",
                    stats.meanRr(), stats.minRr, stats.maxRr));
            sb.append(String.format("BP Sys:    mean %.2f | min %.2f | max %.2f\n",
                    stats.meanSys(), stats.minSys, stats.maxSys));
            sb.append(String.format("BP Dia:    mean %.2f | min %.2f | max %.2f\n\n",
                    stats.meanDia(), stats.minDia, stats.maxDia));
        }

        // -------- Parse abnormal JSON -> highlights --------
        List<AbnormalEvent> abns = parseAbnormalEvents(abnormalJson);
        sb.append("--- Abnormal Highlights (latest first) ---\n");

        int shown = 0;
        int inWindow = 0;
        int urgent = 0;
        int warning = 0;


        for (AbnormalEvent a : abns) {
            long ts = a.timestampMs();
            if (ts < fromMs || ts > toMs) continue;

            inWindow++;
            AlertLevel lvl = a.level();
            if (lvl == AlertLevel.URGENT) urgent++;
            if (lvl == AlertLevel.WARNING) warning++;


            if (shown < MAX_ABNORMAL_LINES) {
                sb.append(fmt.format(Instant.ofEpochMilli(ts)))
                        .append(" | ").append(a.level())
                        .append(" | ").append(a.vitalType())
                        .append(" | ").append(String.format("%.2f", a.value()))
                        .append(" | ").append(a.message())
                        .append("\n");
                shown++;
            }
        }

        if (inWindow == 0) {
            sb.append("No abnormal instances in this window.\n");
        }

        // Overall status
        sb.append("\n");
        sb.append(String.format("Overall: %s (Urgent: %d, Warning: %d, Total: %d)\n\n",
                (urgent > 0 ? "URGENT" : (warning > 0 ? "WARNING" : "STABLE")),
                urgent, warning, inWindow));

        // -------- Optional debug raw JSON --------
        if (REPORT_DEBUG_RAW_JSON) {
            sb.append("---- Raw JSON (Debug) ----\n");
            sb.append("[minutes]\n").append(minutesJson).append("\n\n");
            sb.append("[abnormal]\n").append(abnormalJson).append("\n\n");
        } else {
            sb.append("[Note] Raw minute-by-minute JSON hidden (enable REPORT_DEBUG_RAW_JSON for debugging).\n");
        }

        sb.append("\n===== End =====\n");
        return sb.toString();
    }

    private SummaryStats parseMinutesSummary(String minutesJson, long fromMs, long toMs) {
        SummaryStats s = new SummaryStats();

        try {
            JsonObject root = JsonParser.parseString(minutesJson).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("minutes");
            if (arr == null) return s;

            for (int i = 0; i < arr.size(); i++) {
                JsonObject m = arr.get(i).getAsJsonObject();

                long t = m.has("minuteStartMs") ? m.get("minuteStartMs").getAsLong() : 0L;
                if (t < fromMs || t > toMs) continue;


                double temp = getAsDouble(m, "avgTemp");
                double hr = getAsDouble(m, "avgHr");
                double rr = getAsDouble(m, "avgRr");
                double sys = getAsDouble(m, "avgSys");
                double dia = getAsDouble(m, "avgDia");

                s.add(temp, hr, rr, sys, dia);
            }
        } catch (Exception e) {

            System.err.println("[ReportFrame] parseMinutesSummary failed: " + e.getMessage());
        }

        return s;
    }

    private List<AbnormalEvent> parseAbnormalEvents(String abnormalJson) {
        try {

            AbnormalEvent[] arr = GSON.fromJson(abnormalJson, AbnormalEvent[].class);
            return arr == null ? List.of() : List.of(arr);
        } catch (Exception e) {
            System.err.println("[ReportFrame] parseAbnormalEvents failed: " + e.getMessage());
            return List.of();
        }
    }

    private double getAsDouble(JsonObject obj, String key) {
        try {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return Double.NaN;
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** mean/min/max */
    private static class SummaryStats {
        int total = 0;

        double sumTemp = 0, sumHr = 0, sumRr = 0, sumSys = 0, sumDia = 0;

        double minTemp = Double.POSITIVE_INFINITY, maxTemp = Double.NEGATIVE_INFINITY;
        double minHr = Double.POSITIVE_INFINITY,   maxHr = Double.NEGATIVE_INFINITY;
        double minRr = Double.POSITIVE_INFINITY,   maxRr = Double.NEGATIVE_INFINITY;
        double minSys = Double.POSITIVE_INFINITY,  maxSys = Double.NEGATIVE_INFINITY;
        double minDia = Double.POSITIVE_INFINITY,  maxDia = Double.NEGATIVE_INFINITY;

        void add(double temp, double hr, double rr, double sys, double dia) {

            if (Double.isNaN(temp) || Double.isNaN(hr) || Double.isNaN(rr) || Double.isNaN(sys) || Double.isNaN(dia)) return;

            total++;
            sumTemp += temp; sumHr += hr; sumRr += rr; sumSys += sys; sumDia += dia;

            minTemp = Math.min(minTemp, temp); maxTemp = Math.max(maxTemp, temp);
            minHr = Math.min(minHr, hr);       maxHr = Math.max(maxHr, hr);
            minRr = Math.min(minRr, rr);       maxRr = Math.max(maxRr, rr);
            minSys = Math.min(minSys, sys);    maxSys = Math.max(maxSys, sys);
            minDia = Math.min(minDia, dia);    maxDia = Math.max(maxDia, dia);
        }

        double meanTemp() { return total == 0 ? 0 : sumTemp / total; }
        double meanHr()   { return total == 0 ? 0 : sumHr / total; }
        double meanRr()   { return total == 0 ? 0 : sumRr / total; }
        double meanSys()  { return total == 0 ? 0 : sumSys / total; }
        double meanDia()  { return total == 0 ? 0 : sumDia / total; }
    }


}