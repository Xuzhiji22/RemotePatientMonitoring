package rpm.ui;

import rpm.model.VitalSample;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EcgChartPanel extends JPanel {

    private List<VitalSample> data = List.of();
    private int windowSeconds = 10; // ECG 默认窗口更短更好看

    public EcgChartPanel() {
        setPreferredSize(new Dimension(350, 250));
        setBackground(Color.WHITE);
    }

    public void setWindowSeconds(int s) {
        // ECG 建议不要太长，否则密麻麻；但给你自由
        this.windowSeconds = s;
        repaint();
    }

    public void updateData(List<VitalSample> all) {
        this.data = all;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.BLACK);
        g2.drawString("ECG", 10, 18);

        if (data.isEmpty()) return;

        long latestT = data.get(data.size() - 1).timestampMs();
        long startT = latestT - windowSeconds * 1000L;

        List<VitalSample> win = data.stream()
                .filter(s -> s.timestampMs() >= startT)
                .collect(java.util.stream.Collectors.toList());


        if (win.size() < 2) return;

        double min = win.stream().mapToDouble(VitalSample::ecgValue).min().orElse(-1);
        double max = win.stream().mapToDouble(VitalSample::ecgValue).max().orElse(1);
        if (Math.abs(max - min) < 1e-9) max = min + 1;

        int padLeft = 40, padRight = 10, padTop = 30, padBottom = 20;
        int w = getWidth(), h = getHeight();

        g2.setColor(new Color(230, 230, 230));
        g2.fillRect(padLeft, padTop, w - padLeft - padRight, h - padTop - padBottom);

        g2.setColor(Color.BLACK);
        g2.drawRect(padLeft, padTop, w - padLeft - padRight, h - padTop - padBottom);

        int prevX = -1, prevY = -1;
        for (VitalSample s : win) {
            double v = s.ecgValue();
            double tx = (s.timestampMs() - startT) / (double) (windowSeconds * 1000L);
            double ty = (v - min) / (max - min);

            int x = padLeft + (int) (tx * (w - padLeft - padRight));
            int y = padTop + (int) ((1 - ty) * (h - padTop - padBottom));

            if (prevX >= 0) g2.drawLine(prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }
    }
}
