package rpm.server;

import rpm.dao.VitalSampleDao;
import rpm.model.VitalSample;
import rpm.sim.Simulator;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically generates vital samples on the server and persists them to DB.
 * Only intended to run when Postgres is bound (PG* env vars exist).
 */
public final class ServerSimulationService {

    private final SimulationRegistry registry;
    private final List<String> patientIds;
    private final VitalSampleDao vitalDao = new VitalSampleDao();

    private ScheduledExecutorService scheduler;

    public ServerSimulationService(SimulationRegistry registry, List<String> patientIds) {
        this.registry = registry;
        this.patientIds = patientIds;
    }

    public void start(int hz) {
        if (hz <= 0) hz = 5;

        stop(); // idempotent
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rpm-server-sim");
            t.setDaemon(true);
            return t;
        });

        long periodMs = Math.max(1, 1000L / hz);

        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();

            for (String pid : patientIds) {
                Simulator sim = registry.get(pid);
                if (sim == null) continue;

                VitalSample sample = sim.nextSample(now);

                try {
                    vitalDao.insert(pid, sample);
                } catch (Exception ignored) {
                }
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            try {
                scheduler.shutdownNow();
            } catch (Exception ignored) {
            } finally {
                scheduler = null;
            }
        }
    }
}
