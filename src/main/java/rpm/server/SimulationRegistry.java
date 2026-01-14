package rpm.server;

import rpm.model.Patient;
import rpm.sim.SimpleVitalSimulator;
import rpm.sim.Simulator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds one simulator per patient.
 */
public final class SimulationRegistry {

    private final Map<String, Simulator> sims = new ConcurrentHashMap<>();

    public void ensurePatients(List<Patient> patients) {
        for (Patient p : patients) {
            sims.computeIfAbsent(p.patientId(), id -> new SimpleVitalSimulator(p));
        }
    }

    public Simulator get(String patientId) {
        return sims.get(patientId);
    }
}
