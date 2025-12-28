package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class VitalChartPanel extends JPanel {

    private final String title;
    private final VitalType vitalType;
    private final ToDoubleFunction<VitalSample> extractor;

    private List<VitalSample> data = List.of();
    private int windowSeconds = 30;

    private AlertLevel currentLevel = AlertLevel.NORMAL;
    private double latestValue = Double.NaN;

    public static VitalChartPanel forVital(String title, VitalType type, ToDoubleFunction<VitalSample> extractor) {
        return new VitalChartPanel(title, type, extractor);
    }

    private VitalChartPanel(String title, VitalType vitalType, ToDoubleFunction<VitalSample> extractor) {
        this.title = title;
        this.vitalType = vitalType;
        this.extractor = extractor;
        setPreferredSize(new Dimension(350, 250));
        setBackground(Color.WHITE);
    }

    public void setWindowSeconds(int s) {
        this.windowSeconds = s;
        repaint();
    }

    public void updateData(List<VitalSample> all, AlertEngine alertEngine) {
        this.data = all;
        VitalSample last = all.get(all.size() - 1);
        this.latestValue = extractor.applyAsDouble(last);
        this.currentLevel = alertEngine.eval(vitalType, latestValue);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Border as alarm indicator
        g2.setStroke(new BasicStroke(4));
        g2.setColor(levelColor(currentLevel));
        g2.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.BLACK);
        g2.drawString(title, 10, 18);
        g2.drawString(String.format("Latest: %.2f (%s)", latestValue, currentLevel), 10, 36);

        if (data.isEmpty()) return;

        long latestT = data.get(data.size() - 1).timestampMs();
        long startT = latestT - windowSeconds * 1000L;

        List<VitalSample> win = data.stream()
                .filter(s -> s.timestampMs() >= startT)
                .toList();

        if (win.size() < 2) return;

        double min = win.stream().mapToDouble(extractor).min().orElse(0);
        double max = win.stream().mapToDouble(extractor).max().orElse(1);
        if (Math.abs(max - min) < 1e-9) max = min + 1;

        int padLeft = 40, padRight = 10, padTop = 50, padBottom = 20;
        int w = getWidth(), h = getHeight();

        // axes box
        g2.setColor(new Color(230, 230, 230));
        g2.fillRect(padLeft, padTop, w - padLeft - padRight, h - padTop - padBottom);

        g2.setColor(Color.BLACK);
        g2.drawRect(padLeft, padTop, w - padLeft - padRight, h - padTop - padBottom);

        // polyline
        int prevX = -1, prevY = -1;
        for (VitalSample s : win) {
            double v = extractor.applyAsDouble(s);
            double tx = (s.timestampMs() - startT) / (double) (windowSeconds * 1000L);
            double ty = (v - min) / (max - min);

            int x = padLeft + (int) (tx * (w - padLeft - padRight));
            int y = padTop + (int) ((1 - ty) * (h - padTop - padBottom));

            if (prevX >= 0) {
                g2.setColor(Color.BLACK);
                g2.drawLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
        }

        // min/max labels
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(String.format("%.1f", max), 8, padTop + 10);
        g2.drawString(String.format("%.1f", min), 8, h - padBottom);
    }

    private Color levelColor(AlertLevel level) {
        return switch (level) {
            case NORMAL -> new Color(0, 170, 0);
            case WARNING -> new Color(230, 170, 0);
            case URGENT -> new Color(220, 0, 0);
        };
    }
}
