package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;

import javax.swing.*;

public class NurseOverviewFrame extends BaseOverviewFrame {

    public NurseOverviewFrame(PatientManager pm, AlertEngine alertEngine, JFrame loginFrame) {
        super("Remote Patient Monitor - Overview (NURSE)", pm, alertEngine, loginFrame);
    }

    @Override
    protected JFrame createDetailFrame(Patient patient) {
        return new NurseDetailFrame(pm, alertEngine, patient, this, loginFrame);
    }
}
