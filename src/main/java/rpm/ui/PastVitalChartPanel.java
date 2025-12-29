package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.MinuteRecord;
import rpm.model.AlertLevel;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class PastVitalChartPanel extends JPanel {

    private List<MinuteRecord> data = List.of();
    private int selectedIndex = -1;

    private VitalType vitalType = VitalType.HEART_RATE;
    private ToDoubleFunction<MinuteRecord> extractor = MinuteRecord::avgHR;

    private AlertEngine alertEngine;

    public PastVitalChartPanel(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
        setPreferredSize(new Dimension(950, 420));
        setBackground(Color.WHITE);
    }

    public void setVital(VitalType type, ToDoubleFunction<MinuteRecord> ex) {
        this.vitalType = type;
        this.extractor = ex;
        repaint();
    }

    public void setData(List<MinuteRecord> recs) {
        this.data = recs;
        repaint();
    }

    public void setSelectedIndex(int idx) {
        this.selectedIndex = idx;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padL = 50, padR = 15, padT = 25, padB = 35;
        int w = getWidth(), h = getHeight();
        int plotW = w - padL - padR;
        int plotH = h - padT - padB;

        // background
        g2.setColor(new Color(230, 230, 230));
        g2.fillRect(padL, padT, plotW, plotH);

        g2.setColor(Color.BLACK);
        g2.drawRect(padL, padT, plotW, plotH);

        // min/max
        double min = data.stream().mapToDouble(extractor).min().orElse(0);
        double max = data.stream().mapToDouble(extractor).max().orElse(1);
        if (Math.abs(max - min) < 1e-9) max = min + 1;

        // polyline + abnormal markers
        int n = data.size();
        int prevX = -1, prevY = -1;

        for (int i = 0; i < n; i++) {
            double v = extractor.applyAsDouble(data.get(i));
            double tx = (n <= 1) ? 0 : (i / (double) (n - 1));
            double ty = (v - min) / (max - min);

            int x = padL + (int) (tx * plotW);
            int y = padT + (int) ((1 - ty) * plotH);

            AlertLevel level = alertEngine.eval(vitalType, v);

            // abnormal point in red/orange, normal black
            g2.setColor(level == AlertLevel.URGENT ? new Color(220, 0, 0)
                    : level == AlertLevel.WARNING ? new Color(230, 170, 0)
                    : Color.BLACK);

            if (prevX >= 0) g2.drawLine(prevX, prevY, x, y);

            prevX = x;
            prevY = y;
        }

        // selected minute highlight
        if (selectedIndex >= 0 && selectedIndex < n) {
            double v = extractor.applyAsDouble(data.get(selectedIndex));
            double tx = (n <= 1) ? 0 : (selectedIndex / (double) (n - 1));
            double ty = (v - min) / (max - min);

            int x = padL + (int) (tx * plotW);
            int y = padT + (int) ((1 - ty) * plotH);

            g2.setColor(new Color(0, 90, 255));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x - 6, y - 6, 12, 12);
            g2.setStroke(new BasicStroke(1));
        }

        // labels
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(String.format("max: %.2f", max), 8, padT + 10);
        g2.drawString(String.format("min: %.2f", min), 8, padT + plotH);
        g2.drawString("Last 24 hours (minute averages)", padL, 18);
    }
}
