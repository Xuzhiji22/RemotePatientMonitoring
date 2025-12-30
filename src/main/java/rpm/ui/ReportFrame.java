package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientHistoryStore;
import rpm.model.Patient;
import rpm.report.DailyReport;
import rpm.report.ReportGenerator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportFrame extends JFrame {

    public ReportFrame(Patient patient,
                       PatientHistoryStore history,
                       AlertEngine alertEngine,
                       JFrame parent) {
        super("Report - " + patient.patientId());

        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(textArea);

        JButton back = new JButton("Back");
        back.addActionListener(e -> {
            this.dispose();
            parent.setVisible(true);
        });

        JButton save = new JButton("Save as .txt");
        save.addActionListener(e -> saveToFile(textArea.getText(), patient));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.add(back);
        top.add(save);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Generate report NOW
        ReportGenerator gen = new ReportGenerator();
        DailyReport report = gen.generate24hReport(patient, history.getLast24Hours(), alertEngine);
        String text = gen.toText(report, patient);
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }

    private void saveToFile(String content, Patient patient) {
        JFileChooser chooser = new JFileChooser();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        chooser.setSelectedFile(new java.io.File(patient.patientId() + "_report_" + date + ".txt"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        Path path = chooser.getSelectedFile().toPath();
        try {
            Files.writeString(path, content);
            JOptionPane.showMessageDialog(this, "Saved to:\n" + path, "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
