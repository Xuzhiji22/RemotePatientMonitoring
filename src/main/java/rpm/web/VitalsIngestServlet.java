package rpm.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import rpm.dao.VitalSampleDao;
import rpm.model.VitalSample;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/api/vitals"})
public class VitalsIngestServlet extends HttpServlet {

    private final VitalSampleDao vitalDao = new VitalSampleDao();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        try {
            String body;
            try (BufferedReader br = req.getReader()) {
                body = br.lines().collect(Collectors.joining("\n"));
            }

            if (body == null || body.isBlank()) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "JSON body is required")));
                return;
            }

            JsonObject jo = gson.fromJson(body, JsonObject.class);

            String patientId = getString(jo, "patientId");
            if (patientId == null || patientId.isBlank()) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "patientId is required")));
                return;
            }

            long ts = getLong(jo, "timestampMs", System.currentTimeMillis());
            double temp = getDouble(jo, "bodyTemp", 0);
            double hr = getDouble(jo, "heartRate", 0);
            double rr = getDouble(jo, "respiratoryRate", 0);
            double sys = getDouble(jo, "systolicBP", 0);
            double dia = getDouble(jo, "diastolicBP", 0);
            double ecg = getDouble(jo, "ecgValue", 0);

            VitalSample s = new VitalSample(ts, temp, hr, rr, sys, dia, ecg);
            vitalDao.insert(patientId.trim(), s);

            resp.setStatus(201);
            resp.getWriter().write(gson.toJson(new OkMsg("OK", "inserted", patientId.trim(), ts)));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("VITALS_INGEST_ERROR", e.getMessage())));
        }
    }

    private static String getString(JsonObject jo, String key) {
        return (jo != null && jo.has(key) && !jo.get(key).isJsonNull()) ? jo.get(key).getAsString() : null;
    }

    private static long getLong(JsonObject jo, String key, long def) {
        try {
            return (jo != null && jo.has(key) && !jo.get(key).isJsonNull()) ? jo.get(key).getAsLong() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static double getDouble(JsonObject jo, String key, double def) {
        try {
            return (jo != null && jo.has(key) && !jo.get(key).isJsonNull()) ? jo.get(key).getAsDouble() : def;
        } catch (Exception e) {
            return def;
        }
    }

    static class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) { this.code = code; this.message = message; }
    }

    static class OkMsg {
        String code;
        String message;
        String patientId;
        long timestampMs;
        OkMsg(String code, String message, String patientId, long timestampMs) {
            this.code = code; this.message = message; this.patientId = patientId; this.timestampMs = timestampMs;
        }
    }
}
