package rpm.server;

import rpm.dao.PatientDao;
import rpm.db.Db;
import rpm.db.DbInit;
import rpm.model.Patient;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.List;

@WebListener
public class ServerBootstrapListener implements ServletContextListener {

    public static final String CTX_MINUTE_AGG = "rpm.minute.agg";
    public static final String CTX_SIM_REGISTRY = "rpm.sim.registry";
    public static final String CTX_SIM_SERVICE  = "rpm.sim.service";

    /**
     * Server-side simulator should be OFF by default on cloud.
     * If you want server-only simulation demo, set RPM_SERVER_SIM=1.
     */
    private static boolean serverSimEnabled() {
        String v = System.getenv("RPM_SERVER_SIM");
        return v != null && (v.equalsIgnoreCase("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes"));
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        try {
            SimulationRegistry registry = new SimulationRegistry();
            ctx.setAttribute(CTX_SIM_REGISTRY, registry);

            // Local run without Postgres bound: don't init DB or start DB writers.
            if (!Db.hasPgEnv()) {
                return;
            }

            DbInit.init();

            List<Patient> patients = seedPatients();

            registry.ensurePatients(patients);

            List<String> patientIds = new ArrayList<>();
            for (Patient p : patients) patientIds.add(p.patientId());

            // Server-side simulator OFF by default
            if (serverSimEnabled()) {
                ServerSimulationService sim = new ServerSimulationService(registry, patientIds);
                sim.start(5);
                ctx.setAttribute(CTX_SIM_SERVICE, sim);
            }

            // Minute aggregation ON (needed to compute minute_averages + abnormal_events)
            MinuteAggregationService agg = new MinuteAggregationService(
                    new rpm.dao.VitalSampleDao(),
                    new rpm.dao.MinuteAverageDao(),
                    patientIds
            );
            agg.start();
            ctx.setAttribute(CTX_MINUTE_AGG, agg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        if (ctx == null) return;

        Object a = ctx.getAttribute(CTX_MINUTE_AGG);
        if (a instanceof MinuteAggregationService) {
            try { ((MinuteAggregationService) a).stop(); } catch (Exception ignored) {}
        }

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
