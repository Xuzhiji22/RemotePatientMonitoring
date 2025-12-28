package rpm.ui;

import rpm.model.Patient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class PatientListPanel extends JPanel {
    private Consumer<Patient> onSelected = p -> {};

    public PatientListPanel(List<Patient> patients) {
        setLayout(new BorderLayout());

        DefaultListModel<Patient> model = new DefaultListModel<>();
        for (Patient p : patients) model.addElement(p);

        JList<Patient> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((lst, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.patientId() + " - " + value.name());
            label.setOpaque(true);
            label.setBackground(isSelected ? new Color(200, 220, 255) : Color.WHITE);
            return label;
        });

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                onSelected.accept(list.getSelectedValue());
            }
        });

        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void onPatientSelected(Consumer<Patient> listener) {
        this.onSelected = listener;
    }
}
