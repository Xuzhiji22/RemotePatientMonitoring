package rpm.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import rpm.dao.VitalSampleDao;
import rpm.model.VitalSample;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/api/vitals"})
public class VitalsPostServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final VitalSampleDao vitalDao = new VitalSampleDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        try {
            String body = req.getReader().lines().reduce("", (a, b) -> a + b);
            JsonObject obj = gson.fromJson(body, JsonObject.class);

            String patientId = obj.get("patientId").getAsString();

            VitalSample sample = new VitalSample(
                    obj.get("timestampMs").getAsLong(),
                    obj.get("bodyTemp").getAsDouble(),
                    obj.get("heartRate").getAsDouble(),
                    obj.get("respiratoryRate").getAsDouble(),
                    obj.get("systolicBP").getAsDouble(),
                    obj.get("diastolicBP").getAsDouble(),
                    obj.get("ecgValue").getAsDouble()
            );

            vitalDao.insert(patientId, sample);

            resp.getWriter().write("{\"ok\":true}");
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write(
                    gson.toJson(new ErrorMsg("BAD_REQUEST", e.getMessage()))
            );
        }
    }

    static class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
