package rpm.data;

/**
 * Coordinates simulation, storage and alert evaluation for a single patient.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Receive simulated (or ingested) samples for each vital sign.</li>
 *   <li>Push samples into in-memory ring buffers for real-time plotting.</li>
 *   <li>Aggregate per-minute averages and record abnormal events.</li>
 * </ul>
 */

import rpm.alert.AlertEngine;
import rpm.dao.PatientDao;
import rpm.model.Patient;
import rpm.sim.SimpleVitalSimulator;
import rpm.sim.Simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientManager {

    private final PatientDao patientDao = new PatientDao();

    private final AlertEngine alertEngine;
    private final int maxSeconds;
    private final int sampleHz;

    private final List<Patient> patients = new ArrayList<>();
    private final Map<String, PatientDataStore> stores = new HashMap<>();
    private final Map<String, Simulator> simulators = new HashMap<>();
    private final Map<String, MinuteAggregator> aggregators = new HashMap<>();
    private final Map<String, PatientHistoryStore> historyStores = new HashMap<>();

    public PatientManager(List<Patient> initialPatients, int maxSeconds, int sampleHz, AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
        this.maxSeconds = maxSeconds;
        this.sampleHz = sampleHz;

        for (int i = 0; i < initialPatients.size(); i++) {
            Patient p = initialPatients.get(i);
            internalAddPatient(p, i);
        }
    }


    public synchronized List<Patient> getPatients() {
        return List.copyOf(patients);
    }

    public synchronized PatientDataStore storeOf(String patientId) { return stores.get(patientId); }
    public synchronized Simulator simulatorOf(String patientId) { return simulators.get(patientId); }
    public synchronized MinuteAggregator aggregatorOf(String patientId) { return aggregators.get(patientId); }
    public synchronized PatientHistoryStore historyOf(String patientId) { return historyStores.get(patientId); }

    /** NEW: runtime add patient */
    public synchronized Patient addPatient(String id, String name, int age, String ward, String email, String emergency) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Patient ID cannot be empty");
        if (stores.containsKey(id)) throw new IllegalArgumentException("Patient ID already exists: " + id);

        Patient p = new Patient(id.trim(), name.trim(), age, ward.trim(), email.trim(), emergency.trim());
        internalAddPatient(p, patients.size());
        return p;
    }

    private void internalAddPatient(Patient p, int index) {
        patients.add(p);

        try {
            patientDao.upsert(p);
        } catch (Exception e) {

            e.printStackTrace();
        }

        stores.put(p.patientId(), new PatientDataStore(maxSeconds, sampleHz));

        SimpleVitalSimulator sim = new SimpleVitalSimulator(p);
        //sim.setMode(SimulationMode.NORMAL);
        sim.setHeartRateBase(65 + (index % 10) * 2);
        simulators.put(p.patientId(), sim);

        aggregators.put(p.patientId(), new MinuteAggregator(alertEngine));
        historyStores.put(p.patientId(), new PatientHistoryStore());
    }
}
