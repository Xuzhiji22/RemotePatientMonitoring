package rpm.web;

import com.google.gson.Gson;
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

    private final PatientDao patientDao = new PatientDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        try {
            List<Patient> patients = patientDao.listAll();
            resp.getWriter().write(gson.toJson(patients));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("PATIENTS_API_ERROR", e.getMessage())));
        }
    }

    static class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) { this.code = code; this.message = message; }
    }
}
