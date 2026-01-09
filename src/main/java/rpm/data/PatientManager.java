package rpm.data;

import rpm.alert.AlertEngine;
import rpm.model.Patient;
import rpm.sim.SimpleVitalSimulator;
import rpm.sim.SimulationMode;
import rpm.sim.Simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rpm.dao.PatientDao;

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

    // 你原来有 getPatients() 的话，建议返回 copy，避免 UI/线程误改
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
            // 本地没绑 PG 环境变量也能继续跑，不要让它直接炸掉
            e.printStackTrace();
        }

        stores.put(p.patientId(), new PatientDataStore(maxSeconds, sampleHz));

        SimpleVitalSimulator sim = new SimpleVitalSimulator(p);
        sim.setMode(index % 5 == 0 ? SimulationMode.ABNORMAL : SimulationMode.NORMAL);
        sim.setHeartRateBase(65 + index * 3);
        simulators.put(p.patientId(), sim);

        aggregators.put(p.patientId(), new MinuteAggregator(alertEngine));
        historyStores.put(p.patientId(), new PatientHistoryStore());
    }
}
