package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;

import javax.swing.*;

public class DoctorOverviewFrame extends BaseOverviewFrame {

    public DoctorOverviewFrame(PatientManager pm, AlertEngine alertEngine, JFrame loginFrame) {
        super("Remote Patient Monitor - Overview (DOCTOR)", pm, alertEngine, loginFrame);
    }

    @Override
    protected JFrame createDetailFrame(Patient patient) {
        return new DoctorDetailFrame(pm, alertEngine, patient, this, loginFrame);
    }
}
