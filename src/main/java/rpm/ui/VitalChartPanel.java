package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.model.AlertLevel;
import rpm.model.VitalSample;
import rpm.model.VitalType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class VitalChartPanel extends JPanel {

    private final String title;
    private final VitalType vitalType;
    private final ToDoubleFunction<VitalSample> extractor;

    // live state (updated by updateData)
    private List<VitalSample> data = List.of();
    private int windowSeconds = 30;

    private AlertLevel currentLevel = AlertLevel.NORMAL;
    private double latestValue = Double.NaN;

    // render style
    private final boolean largeMode;

    public static VitalChartPanel forVital(String title, VitalType type, ToDoubleFunction<VitalSample> extractor) {
        return new VitalChartPanel(title, type, extractor, false);
    }

    private VitalChartPanel(String title, VitalType vitalType, ToDoubleFunction<VitalSample> extractor, boolean largeMode) {
        this.title = title;
        this.vitalType = vitalType;
        this.extractor = extractor;
        this.largeMode = largeMode;

        setBackground(Color.WHITE);

        if (largeMode) {
            setPreferredSize(new Dimension(950, 600));
        } else {
            setPreferredSize(new Dimension(350, 250));

            // small chart: click to open zoomed dialog
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to enlarge");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openZoomDialog();
                }
            });
        }
    }

    public void setWindowSeconds(int s) {
        this.windowSeconds = s;
        repaint();
    }

    public void updateData(List<VitalSample> all, AlertEngine alertEngine) {
        this.data = all;
        if (all == null || all.isEmpty()) return;

        VitalSample last = all.get(all.size() - 1);
        this.latestValue = extractor.applyAsDouble(last);
        this.currentLevel = alertEngine.eval(vitalType, latestValue);
        repaint();
    }

    //  Zoom logic

    private VitalChartPanel createZoomedCopy() {
        VitalChartPanel big = new VitalChartPanel(title, vitalType, extractor, true);
        copyStateTo(big);
        return big;
    }

    private void copyStateTo(VitalChartPanel other) {
        other.windowSeconds = this.windowSeconds;
        other.data = this.data;
        other.currentLevel = this.currentLevel;
        other.latestValue = this.latestValue;
    }

    private void openZoomDialog() {
        final VitalChartPanel small = VitalChartPanel.this;
        final VitalChartPanel big = createZoomedCopy();

        Window owner = SwingUtilities.getWindowAncestor(small);
        final JDialog dlg = new JDialog(owner, title + " (Zoom)", Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(big, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(ev -> dlg.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);
        root.add(bottom, BorderLayout.SOUTH);

        dlg.setContentPane(root);

        // keep big chart updating from the small chart (Swing Timer runs on EDT)
        final Timer t = new Timer(200, ev -> {
            small.copyStateTo(big);
            big.repaint();
        });
        t.start();

        // stop timer when dialog closes
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                t.stop();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                t.stop();
            }
        });

        dlg.getRootPane().registerKeyboardAction(
                ev -> dlg.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    //  Painting

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Border as alarm indicator
        g2.setStroke(new BasicStroke(largeMode ? 6 : 4));
        g2.setColor(levelColor(currentLevel));
        g2.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        // fonts
        Font titleFont = largeMode ? new Font("SansSerif", Font.BOLD, 18)
                : new Font("SansSerif", Font.BOLD, 12);
        Font infoFont = largeMode ? new Font("SansSerif", Font.PLAIN, 16)
                : new Font("SansSerif", Font.PLAIN, 11);
        Font axisFont = largeMode ? new Font("SansSerif", Font.PLAIN, 14)
                : new Font("SansSerif", Font.PLAIN, 10);

        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.BLACK);

        g2.setFont(titleFont);
        g2.drawString(title, 10, largeMode ? 28 : 18);

        g2.setFont(infoFont);
        g2.drawString(String.format("Latest: %.2f (%s)", latestValue, currentLevel),
                10, largeMode ? 55 : 36);

        if (data == null || data.isEmpty()) {
            g2.dispose();
            return;
        }

        long latestT = data.get(data.size() - 1).timestampMs();
        long startT = latestT - windowSeconds * 1000L;

        List<VitalSample> win = data.stream()
                .filter(s -> s.timestampMs() >= startT)
                .collect(Collectors.toList());

        if (win.size() < 2) {
            g2.dispose();
            return;
        }

        double min = win.stream().mapToDouble(extractor).min().orElse(0);
        double max = win.stream().mapToDouble(extractor).max().orElse(1);
        if (Math.abs(max - min) < 1e-9) max = min + 1;

        // expand range so the signal doesn't touch top/bottom ("headroom")
        double span = max - min;
        double pad = span * 0.10;            // 10% headroom
        if (pad < 1e-6) pad = 1.0;           // safety
        double plotMin = min - pad;
        double plotMax = max + pad;

        int padLeft = largeMode ? 90 : 40;    // more room for y labels in zoom
        int padRight = largeMode ? 20 : 10;
        int padTop = largeMode ? 90 : 50;
        int padBottom = largeMode ? 70 : 20;  // more room for x labels in zoom
        int w = getWidth(), h = getHeight();

        int plotW = w - padLeft - padRight;
        int plotH = h - padTop - padBottom;

        // plot background
        g2.setColor(new Color(235, 235, 235));
        g2.fillRect(padLeft, padTop, plotW, plotH);

        // plot border
        g2.setColor(Color.BLACK);
        g2.drawRect(padLeft, padTop, plotW, plotH);

        // clearer axes in zoom: ticks + labels + grid
        g2.setFont(axisFont);

        if (largeMode) {
            int tickCount = 5; // 0%,25%,50%,75%,100%

            FontMetrics fm = g2.getFontMetrics();

            // horizontal grid + y labels
            g2.setColor(new Color(200, 200, 200));
            for (int i = 0; i < tickCount; i++) {
                double frac = i / (double) (tickCount - 1); // 0..1 top->bottom
                int y = padTop + (int) (frac * plotH);
                g2.drawLine(padLeft, y, padLeft + plotW, y);
            }

            g2.setColor(Color.DARK_GRAY);
            for (int i = 0; i < tickCount; i++) {
                double frac = i / (double) (tickCount - 1);
                double val = plotMax - frac * (plotMax - plotMin); // top is max
                String sVal = String.format("%.1f", val);

                int y = padTop + (int) (frac * plotH);
                int textW = fm.stringWidth(sVal);
                int textX = padLeft - 12 - textW;
                int textY = y + fm.getAscent() / 2 - 2;

                g2.drawString(sVal, textX, textY);
                g2.drawLine(padLeft - 5, y, padLeft, y); // y tick mark
            }

            // vertical grid + x labels
            g2.setColor(new Color(210, 210, 210));
            for (int i = 0; i < tickCount; i++) {
                double frac = i / (double) (tickCount - 1);
                int x = padLeft + (int) (frac * plotW);
                g2.drawLine(x, padTop, x, padTop + plotH);
            }

            g2.setColor(Color.DARK_GRAY);
            for (int i = 0; i < tickCount; i++) {
                double frac = i / (double) (tickCount - 1);
                int secAgo = (int) Math.round((1 - frac) * windowSeconds);
                String sSec = String.valueOf(secAgo);

                int x = padLeft + (int) (frac * plotW);
                int textW = fm.stringWidth(sSec);
                int textX = x - textW / 2;
                int textY = padTop + plotH + fm.getAscent() + 10;

                g2.drawString(sSec, textX, textY);
                g2.drawLine(x, padTop + plotH, x, padTop + plotH + 5); // x tick mark
            }

            String xLabel = "Time (s ago)";
            int xLabelW = fm.stringWidth(xLabel);
            g2.drawString(xLabel, padLeft + (plotW - xLabelW) / 2, h - 12);

            // y label (simple; title already contains unit like bpm/°C/mmHg)
            g2.drawString("Value", 12, padTop - 10);
        }

        // polyline
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(largeMode ? 2.0f : 1.0f));

        int prevX = -1, prevY = -1;
        for (VitalSample s : win) {
            double v = extractor.applyAsDouble(s);
            double tx = (s.timestampMs() - startT) / (double) (windowSeconds * 1000L);

            // use expanded range plotMin/plotMax
            double ty = (v - plotMin) / (plotMax - plotMin);

            int x = padLeft + (int) (tx * plotW);
            int y = padTop + (int) ((1 - ty) * plotH);

            if (prevX >= 0) {
                g2.drawLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
        }

        // small mode: keep simple min/max labels (using expanded range for nicer look)
        if (!largeMode) {
            g2.setFont(axisFont);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(String.format("%.1f", plotMax), 8, padTop + 10);
            g2.drawString(String.format("%.1f", plotMin), 8, h - padBottom);
        }

        g2.dispose();
    }

    private Color levelColor(AlertLevel level) {
        switch (level) {
            case NORMAL:
                return new Color(0, 170, 0);
            case WARNING:
                return new Color(230, 170, 0);
            case URGENT:
                return new Color(220, 0, 0);
            default:
                return Color.BLACK;
        }
    }
}
