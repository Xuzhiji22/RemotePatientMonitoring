package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;

import javax.swing.*;
import java.awt.*;

public abstract class BaseOverviewFrame extends JFrame {

    protected final PatientManager pm;
    protected final AlertEngine alertEngine;
    protected final JFrame loginFrame;

    public BaseOverviewFrame(String title, PatientManager pm, AlertEngine alertEngine, JFrame loginFrame) {
        super(title);
        this.pm = pm;
        this.alertEngine = alertEngine;
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        for (Patient p : pm.getPatients()) {
            PatientTilePanel tile = new PatientTilePanel(p, pm, alertEngine);
            tile.onOpenDetail(() -> openDetail(p));
            grid.add(tile);
        }

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            this.dispose();
            loginFrame.setVisible(true);
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(logout);
        top.add(extraTopControls()); // 子类可加额外控件

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
    }

    private void openDetail(Patient p) {
        JFrame detail = createDetailFrame(p);
        detail.setVisible(true);
        this.setVisible(false);
    }

    //子类决定打开哪个 detail frame
    protected abstract JFrame createDetailFrame(Patient patient);

    //子类可在顶部加按钮/标签；默认空 panel
    protected JComponent extraTopControls() {
        return new JPanel(); // empty
    }
}
