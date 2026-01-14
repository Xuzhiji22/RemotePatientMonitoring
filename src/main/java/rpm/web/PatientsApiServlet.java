package rpm.web;

import rpm.dao.PatientDao;
import rpm.model.Patient;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/patients"})
public class PatientsApiServlet extends HttpServlet {

    private static boolean hasPgEnv() {
        return System.getenv("PGHOST") != null
                && System.getenv("PGPORT") != null
                && System.getenv("PGDATABASE") != null
                && System.getenv("PGUSER") != null
                && System.getenv("PGPASSWORD") != null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");

        // LOCAL：DB disabled -> []
        if (!hasPgEnv()) {
            resp.getWriter().write("[]");
            return;
        }

        try {
            PatientDao dao = new PatientDao();
            List<Patient> patients = dao.listAll();

            resp.getWriter().write(toJson(patients));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private static String toJson(List<Patient> patients) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
                    .append("\"patientId\":\"").append(escape(p.patientId())).append("\",")
                    .append("\"name\":\"").append(escape(p.name())).append("\",")
                    .append("\"age\":").append(p.age()).append(",")
                    .append("\"ward\":\"").append(escape(p.ward())).append("\",")
                    .append("\"email\":\"").append(escape(p.email())).append("\",")
                    .append("\"emergencyContact\":\"").append(escape(p.emergencyContact())).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
