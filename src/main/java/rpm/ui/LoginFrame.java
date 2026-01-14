package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.auth.AuthService;
import rpm.auth.UserAccount;
import rpm.auth.UserRole;
import rpm.auth.UserStore;
import rpm.config.ConfigStore;
import rpm.data.PatientManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginFrame extends JFrame {

    private final AuthService auth;
    private final PatientManager pm;
    private final AlertEngine alertEngine;
    private final UserStore userStore;
    private final ConfigStore configStore;

    private final Runnable onLoginSuccess; // arm audio
    private final Runnable onLogout;       // disarm audio

    public LoginFrame(PatientManager pm,
                      AlertEngine alertEngine,
                      AuthService auth,
                      UserStore userStore,
                      ConfigStore configStore,
                      Runnable onLoginSuccess,
                      Runnable onLogout) {

        super("Remote Patient Monitor - Login");

        this.pm = pm;
        this.alertEngine = alertEngine;
        this.auth = auth;
        this.userStore = userStore;
        this.configStore = configStore;
        this.onLoginSuccess = onLoginSuccess;
        this.onLogout = onLogout;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 240);
        setLocationRelativeTo(null);

        JTextField userField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);

        JButton loginBtn = new JButton("Login");

        JLabel hint = new JLabel("<html><body style='color:gray'>Demo accounts:<br>" +
                "admin/admin123<br>doctor/doctor123<br>nurse/nurse123</body></html>");

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());

            var opt = auth.authenticate(u, p);
            if (opt.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserAccount account = opt.get();

            // Arm audio only after successful login
            if (onLoginSuccess != null) {
                try { onLoginSuccess.run(); } catch (Exception ex) {
                    System.err.println("[LoginFrame] onLoginSuccess failed: " + ex.getMessage());
                }
            }

            route(account.role());
        });

        passField.addActionListener(e -> loginBtn.doClick());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Username:"), c);
        c.gridx = 1;
        form.add(userField, c);

        c.gridx = 0; c.gridy = 1;
        form.add(new JLabel("Password:"), c);
        c.gridx = 1;
        form.add(passField, c);

        c.gridx = 1; c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        form.add(loginBtn, c);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.CENTER);
        root.add(hint, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void route(UserRole role) {
        this.setVisible(false);

        JFrame child;

        if (role == UserRole.ADMINISTRATOR) {
            child = new AdminFrame(pm, alertEngine, this, userStore, configStore);
        } else if (role == UserRole.DOCTOR) {
            child = new DoctorOverviewFrame(pm, alertEngine, this);
        } else {
            child = new NurseOverviewFrame(pm, alertEngine, this);
        }

        // When user closes/leaves the child window, disarm audio
        child.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (onLogout != null) onLogout.run();
            }
            @Override
            public void windowClosing(WindowEvent e) {
                if (onLogout != null) onLogout.run();
            }
        });

        child.setVisible(true);
    }
}
