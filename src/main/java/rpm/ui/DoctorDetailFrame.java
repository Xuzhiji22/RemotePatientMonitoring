package rpm.ui;

import rpm.alert.AlertEngine;
import rpm.data.PatientManager;
import rpm.model.Patient;

import javax.swing.*;
import java.awt.*;

public class DoctorDetailFrame extends BaseDetailFrame {

    public DoctorDetailFrame(PatientManager pm, AlertEngine alertEngine, Patient patient,
                             JFrame overviewFrame, JFrame loginFrame) {
        super("Patient Detail - " + patient.patientId() + " (DOCTOR)",
                pm, alertEngine, patient, overviewFrame, loginFrame);
    }

    @Override
    protected JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));

        JButton viewPastData = new JButton("View Past Data");
        viewPastData.addActionListener(e -> {
            PastDataFrame past = new PastDataFrame(
                    patient,
                    pm.historyOf(patient.patientId()),
                    alertEngine,
                    this
            );
            past.setVisible(true);
            this.setVisible(false);
        });

        JButton pastAbn = new JButton("Past Abnormal");
        pastAbn.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Past Abnormal: implement later", "Info", JOptionPane.INFORMATION_MESSAGE)
        );

        JButton genRep = new JButton("Generate Report");
        genRep.addActionListener(e -> {
            ReportFrame rf = new ReportFrame(
                    patient,
                    pm.historyOf(patient.patientId()),
                    alertEngine,
                    this
            );
            rf.setVisible(true);
            this.setVisible(false);
        });

        bottom.add(viewPastData);
        bottom.add(pastAbn);
        bottom.add(genRep);
        return bottom;
    }

    @Override
    protected String viewName() {
        return "Doctor";
    }
}
