package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.dao.AbnormalEventDao;
import rpm.dao.MinuteAverageDao;
import rpm.data.AbnormalEvent;
import rpm.data.MinuteRecord;
import rpm.data.PatientHistoryStore;
import rpm.model.Patient;

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
        generateLast24hReport(); // 打开就先生成一份
    }

    private void buildUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnGen = new JButton("Generate (Last 24h)");
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
        long now = System.currentTimeMillis();
        long from = now - 24L * 60L * 60L * 1000L;

        try {
            MinuteAverageDao minuteDao = new MinuteAverageDao();
            AbnormalEventDao abnormalDao = new AbnormalEventDao();

            // 你们 minute_averages 是按 minute_start_ms 存的，这里取最多 24h * 60 = 1440
            List<MinuteRecord> minutes = minuteDao.latest(patient.patientId(), 2000);
            List<AbnormalEvent> abns = abnormalDao.latest(patient.patientId(), 2000);

            String report = buildTextReport(patient, from, now, minutes, abns);
            textArea.setText(report);
            textArea.setCaretPosition(0);

        } catch (Exception ex) {
            textArea.setText("Failed to generate report: " + ex.getMessage());
        }
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
}
