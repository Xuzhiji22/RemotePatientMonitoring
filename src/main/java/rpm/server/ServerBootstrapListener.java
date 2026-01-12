package rpm.server;

import rpm.dao.PatientDao;
import rpm.db.DbInit;
import rpm.model.Patient;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstraps server-side components on webapp startup:
 * - DB schema init (DbInit)
 * - Seed 8 patients into DB
 * - Start server-side simulation service (writes to DB periodically)
 *
 * This is required for a real Level 3 architecture:
 * Client reads from server API; server owns persistence & simulation.
 */
@WebListener
public class ServerBootstrapListener implements ServletContextListener {

    public static final String CTX_SIM_REGISTRY = "rpm.sim.registry";
    public static final String CTX_SIM_SERVICE  = "rpm.sim.service";

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Local run without Postgres bound: do nothing (keep UI runnable)
        if (!hasPgEnv()) return;

        ServletContext ctx = sce.getServletContext();

        try {
            // 1) init schema
            DbInit.init();

            // 2) seed patients
            List<Patient> patients = seedPatients();

            // 3) start server-side simulator
            SimulationRegistry registry = new SimulationRegistry();
            registry.ensurePatients(patients);

            List<String> patientIds = new ArrayList<>();
            for (Patient p : patients) patientIds.add(p.patientId());

            ServerSimulationService sim = new ServerSimulationService(registry, patientIds);
            sim.start(5);

            ctx.setAttribute(CTX_SIM_REGISTRY, registry);
            ctx.setAttribute(CTX_SIM_SERVICE, sim);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        Object o = ctx.getAttribute(CTX_SIM_SERVICE);
        if (o instanceof ServerSimulationService) {
            try { ((ServerSimulationService) o).stop(); } catch (Exception ignored) {}
        }
    }

    private List<Patient> seedPatients() {
        PatientDao dao = new PatientDao();

        List<Patient> patients = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            String id = String.format("P%03d", i);

            Patient p = new Patient(
                    id,
                    "Patient " + i,
                    20 + i,
                    "Ward A",
                    "patient" + i + "@example.com",
                    "999-000-" + String.format("%04d", i)
            );

            dao.upsert(p);
            patients.add(p);
        }

        return patients;
    }

}
