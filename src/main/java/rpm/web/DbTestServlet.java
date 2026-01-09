package rpm.web;

import rpm.dao.PatientDao;
import rpm.dao.VitalSampleDao;
import rpm.model.Patient;
import rpm.model.VitalSample;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/dbtest"})
public class DbTestServlet extends HttpServlet {

    private final PatientDao patientDao = new PatientDao();
    private final VitalSampleDao vitalDao = new VitalSampleDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; charset=utf-8");

        String patientId = req.getParameter("patientId");
        if (patientId == null || patientId.isBlank()) patientId = "P001";

        long now = System.currentTimeMillis();

        // 1) upsert patient
        Patient p = new Patient(
                patientId,
                "Test Patient",
                20,
                "Ward A",
                "test@example.com",
                "999-999"
        );

        // 2) insert one vital sample
        VitalSample s = new VitalSample(
                now,
                36.7,
                75.0,
                14.0,
                120.0,
                80.0,
                0.12
        );

        try {
            patientDao.upsert(p);
            vitalDao.insert(patientId, s);

            // 3) read latest
            List<VitalSample> latest = vitalDao.latest(patientId, 1);

            resp.getWriter().println("OK - wrote 1 sample for patientId=" + patientId);
            resp.getWriter().println("Latest size=" + latest.size());
            if (!latest.isEmpty()) {
                VitalSample x = latest.get(0);
                resp.getWriter().println("Latest ts=" + x.timestampMs()
                        + " temp=" + x.bodyTemp()
                        + " hr=" + x.heartRate()
                        + " rr=" + x.respiratoryRate()
                        + " sys=" + x.systolicBP()
                        + " dia=" + x.diastolicBP()
                        + " ecg=" + x.ecgValue());
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().println("DBTEST ERROR: " + e.getMessage());
            e.printStackTrace(resp.getWriter());
        }
    }
}
