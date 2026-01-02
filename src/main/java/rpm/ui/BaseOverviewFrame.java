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

    private final JPanel tilesPanel = new JPanel(new GridLayout(0, 4, 12, 12)); // 0 rows => auto grow

    public BaseOverviewFrame(String title, PatientManager pm, AlertEngine alertEngine, JFrame loginFrame) {
        super(title);
        this.pm = pm;
        this.alertEngine = alertEngine;
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);

        tilesPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // initial tiles
        for (Patient p : pm.getPatients()) {
            tilesPanel.add(makeTile(p));
        }

        JScrollPane scroll = new JScrollPane(tilesPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // top bar
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            this.dispose();
            loginFrame.setVisible(true);
        });

        JButton addPatientBtn = new JButton("Add Patient");
        addPatientBtn.addActionListener(e -> onAddPatient());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(logout);
        top.add(addPatientBtn);
        top.add(extraTopControls());

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private PatientTilePanel makeTile(Patient p) {
        PatientTilePanel tile = new PatientTilePanel(p, pm, alertEngine);
        tile.onOpenDetail(() -> {
            JFrame detail = createDetailFrame(p);
            detail.setVisible(true);
            this.setVisible(false);
        });
        return tile;
    }

    private void onAddPatient() {
        // simple input dialog (no new class needed)
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField age = new JTextField();
        JTextField ward = new JTextField();
        JTextField email = new JTextField();
        JTextField emergency = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Patient ID:")); form.add(id);
        form.add(new JLabel("Name:")); form.add(name);
        form.add(new JLabel("Age:")); form.add(age);
        form.add(new JLabel("Ward:")); form.add(ward);
        form.add(new JLabel("Email:")); form.add(email);
        form.add(new JLabel("Emergency contact:")); form.add(emergency);

        int ok = JOptionPane.showConfirmDialog(
                this, form, "Add Patient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            int ageInt = Integer.parseInt(age.getText().trim());
            Patient p = pm.addPatient(
                    id.getText(), name.getText(), ageInt,
                    ward.getText(), email.getText(), emergency.getText()
            );

            // add tile to UI
            tilesPanel.add(makeTile(p));
            tilesPanel.revalidate();
            tilesPanel.repaint();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Age must be an integer.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Add Patient", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to add patient: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // The subclass determines which detail frame
    protected abstract JFrame createDetailFrame(Patient patient);

    // Subclasses can add extra controls at the top; the default is empty
    protected JComponent extraTopControls() {
        return new JPanel();
    }
}
