package rpm.data;

import rpm.alert.AlertEngine;
import rpm.model.Patient;
import rpm.sim.SimpleVitalSimulator;
import rpm.sim.SimulationMode;
import rpm.sim.Simulator;

import java.util.*;

public class PatientManager {

    private final List<Patient> patients;
    private final Map<String, PatientDataStore> stores = new HashMap<>();
    private final Map<String, Simulator> simulators = new HashMap<>();

    // NEW
    private final Map<String, MinuteAggregator> aggregators = new HashMap<>();
    private final Map<String, PatientHistoryStore> historyStores = new HashMap<>();

    public PatientManager(List<Patient> patients, int maxSeconds, int sampleHz, AlertEngine alertEngine) {
        this.patients = List.copyOf(patients);

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);

            stores.put(p.patientId(), new PatientDataStore(maxSeconds, sampleHz));

            SimpleVitalSimulator sim = new SimpleVitalSimulator();
            sim.setMode(i % 5 == 0 ? SimulationMode.ABNORMAL : SimulationMode.NORMAL);
            sim.setHeartRateBase(65 + i * 3);
            simulators.put(p.patientId(), sim);

            aggregators.put(p.patientId(), new MinuteAggregator(alertEngine));
            historyStores.put(p.patientId(), new PatientHistoryStore());
        }
    }

    public List<Patient> getPatients() { return patients; }

    public PatientDataStore storeOf(String patientId) { return stores.get(patientId); }

    public Simulator simulatorOf(String patientId) { return simulators.get(patientId); }

    public MinuteAggregator aggregatorOf(String patientId) { return aggregators.get(patientId); }

    public PatientHistoryStore historyOf(String patientId) { return historyStores.get(patientId); }
}
