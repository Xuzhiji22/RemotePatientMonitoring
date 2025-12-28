package rpm.data;

import rpm.model.Patient;
import rpm.sim.SimpleVitalSimulator;
import rpm.sim.SimulationMode;
import rpm.sim.Simulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientManager {

    private final List<Patient> patients;
    private final Map<String, PatientDataStore> stores = new HashMap<>();
    private final Map<String, Simulator> simulators = new HashMap<>();

    public PatientManager(List<Patient> patients, int maxSeconds, int sampleHz) {
        this.patients = List.copyOf(patients);

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            stores.put(p.patientId(), new PatientDataStore(maxSeconds, sampleHz));

            // 每个病人一个 simulator（参数稍微不同，看起来像不同人）
            SimpleVitalSimulator sim = new SimpleVitalSimulator();
            sim.setMode(i % 5 == 0 ? SimulationMode.ABNORMAL : SimulationMode.NORMAL); // 每 5 个来一个 abnormal demo
            sim.setHeartRateBase(65 + i * 3);
            simulators.put(p.patientId(), sim);
        }
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public PatientDataStore storeOf(String patientId) {
        return stores.get(patientId);
    }

    public Simulator simulatorOf(String patientId) {
        return simulators.get(patientId);
    }
}
