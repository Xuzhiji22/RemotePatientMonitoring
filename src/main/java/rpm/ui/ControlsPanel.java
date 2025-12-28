package rpm.ui;

import rpm.sim.SimulationMode;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ControlsPanel extends JPanel {

    private Consumer<Integer> windowSecondsListener = s -> {};
    private Consumer<SimulationMode> modeListener = m -> {};
    private Consumer<Double> hrBaseListener = bpm -> {};

    private final JSlider windowSlider = new JSlider(10, 300, 30);
    private final JComboBox<SimulationMode> modeBox = new JComboBox<>(SimulationMode.values());
    private final JSpinner hrBaseSpinner = new JSpinner(new SpinnerNumberModel(75.0, 30.0, 200.0, 1.0));

    public ControlsPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));

        add(new JLabel("Window (seconds):"));
        windowSlider.setPreferredSize(new Dimension(220, 40));
        windowSlider.addChangeListener(e -> windowSecondsListener.accept(windowSlider.getValue()));
        add(windowSlider);

        add(new JLabel("Simulation mode:"));
        modeBox.addActionListener(e -> modeListener.accept((SimulationMode) modeBox.getSelectedItem()));
        add(modeBox);

        add(new JLabel("HR base (bpm):"));
        hrBaseSpinner.addChangeListener(e -> hrBaseListener.accept((Double) hrBaseSpinner.getValue()));
        add(hrBaseSpinner);
    }

    public void onWindowSecondsChanged(Consumer<Integer> listener) {
        this.windowSecondsListener = listener;
    }

    public void onModeChanged(Consumer<SimulationMode> listener) {
        this.modeListener = listener;
    }

    public void onHrBaseChanged(Consumer<Double> listener) {
        this.hrBaseListener = listener;
    }
}
