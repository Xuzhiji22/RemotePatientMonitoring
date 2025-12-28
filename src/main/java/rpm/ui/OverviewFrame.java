package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;

import javax.swing.*;
import java.awt.*;

public class OverviewFrame extends JFrame {

    public OverviewFrame(PatientManager pm, AlertEngine alertEngine) {
        super("Remote Patient Monitor - Overview");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        for (Patient p : pm.getPatients()) {
            PatientTilePanel tile = new PatientTilePanel(p, pm, alertEngine);
            tile.onOpenDetail(() -> openDetail(pm, alertEngine, p));
            grid.add(tile);
        }

        JButton selectBtn = new JButton("Select");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(selectBtn); // 你可以以后改成筛选/搜索

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
    }

    private void openDetail(PatientManager pm, AlertEngine alertEngine, Patient patient) {
        DetailFrame detail = new DetailFrame(pm, alertEngine, patient, this);
        detail.setVisible(true);
        this.setVisible(false);
    }
}
