package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LatestReadingsPanel extends JPanel {

    private final JLabel updatedLabel = new JLabel("-");
    private final ReadingCard temp = new ReadingCard("Temperature", "°C");
    private final ReadingCard hr   = new ReadingCard("Heart Rate", "bpm");
    private final ReadingCard rr   = new ReadingCard("Resp Rate", "rpm");
    private final ReadingCard bp   = new ReadingCard("Blood Pressure", "mmHg");

    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LatestReadingsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Latest Readings"));

        updatedLabel.setForeground(Color.GRAY);
        add(row(updatedLabel));
        add(Box.createVerticalStrut(8));

        add(temp);
        add(Box.createVerticalStrut(8));
        add(hr);
        add(Box.createVerticalStrut(8));
        add(rr);
        add(Box.createVerticalStrut(8));
        add(bp);
        add(Box.createVerticalGlue());
    }

    private JPanel row(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.add(c);
        return p;
    }

    public void update(VitalSample s, AlertEngine alertEngine) {
        updatedLabel.setText("Updated: " + fmt.format(new Date(s.timestampMs())));

        AlertLevel tLv  = alertEngine.eval(VitalType.BODY_TEMPERATURE, s.bodyTemp());
        AlertLevel hLv  = alertEngine.eval(VitalType.HEART_RATE, s.heartRate());
        AlertLevel rLv  = alertEngine.eval(VitalType.RESPIRATORY_RATE, s.respiratoryRate());
        AlertLevel sys  = alertEngine.eval(VitalType.SYSTOLIC_BP, s.systolicBP());
        AlertLevel dia  = alertEngine.eval(VitalType.DIASTOLIC_BP, s.diastolicBP());
        AlertLevel bpLv = worst(sys, dia);

        temp.setValue(String.format("%.2f", s.bodyTemp()), tLv);
        hr.setValue(String.format("%.2f", s.heartRate()), hLv);
        rr.setValue(String.format("%.2f", s.respiratoryRate()), rLv);
        bp.setValue(String.format("%.0f/%.0f", s.systolicBP(), s.diastolicBP()), bpLv);
    }

    private AlertLevel worst(AlertLevel a, AlertLevel b) {
        // URGENT > WARNING > NORMAL
        if (a == AlertLevel.URGENT || b == AlertLevel.URGENT) return AlertLevel.URGENT;
        if (a == AlertLevel.WARNING || b == AlertLevel.WARNING) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }

    private static class ReadingCard extends JPanel {
        private final JLabel title = new JLabel();
        private final JLabel value = new JLabel("-");
        private final JLabel status = new JLabel("NORMAL");
        private final String unit;

        ReadingCard(String name, String unit) {
            this.unit = unit;
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 210, 210)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            title.setText(name);
            title.setFont(new Font("SansSerif", Font.BOLD, 13));

            value.setFont(new Font("SansSerif", Font.BOLD, 22));
            status.setFont(new Font("SansSerif", Font.PLAIN, 12));

            add(title, BorderLayout.NORTH);

            JPanel mid = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            mid.add(value);
            JLabel unitLabel = new JLabel(unit);
            unitLabel.setForeground(Color.GRAY);
            mid.add(unitLabel);
            add(mid, BorderLayout.CENTER);

            add(status, BorderLayout.SOUTH);

            setStatus(AlertLevel.NORMAL);
        }

        void setValue(String v, AlertLevel level) {
            value.setText(v);
            setStatus(level);
        }

        void setStatus(AlertLevel level) {
            status.setText(level.toString());
            if (level == AlertLevel.URGENT) {
                status.setForeground(new Color(180, 0, 0));
            } else if (level == AlertLevel.WARNING) {
                status.setForeground(new Color(180, 120, 0));
            } else {
                status.setForeground(new Color(0, 140, 0));
            }
        }
    }
}
