package rpm.web;

import com.google.gson.Gson;
import rpm.dao.VitalSampleDao;
import rpm.model.VitalSample;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/vitals/latest"})
public class VitalsApiServlet extends HttpServlet {

    private final VitalSampleDao vitalDao = new VitalSampleDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        String patientId = req.getParameter("patientId");
        String limitStr = req.getParameter("limit");

        int limit = 50;
        if (limitStr != null) {
            try { limit = Integer.parseInt(limitStr); } catch (Exception ignored) {}
        }

        if (patientId == null || patientId.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "patientId is required")));
            return;
        }

        try {
            List<VitalSample> samples = vitalDao.latest(patientId.trim(), limit);
            resp.getWriter().write(gson.toJson(samples));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("VITALS_API_ERROR", e.getMessage())));
        }
    }

    static class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) { this.code = code; this.message = message; }
    }
}
