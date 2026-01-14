package rpm.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import rpm.dao.MinuteAverageDao;
import rpm.data.MinuteRecord;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/api/minutes"})
public class MinutesIngestServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final MinuteAverageDao dao = new MinuteAverageDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        try {
            String body;
            try (BufferedReader br = req.getReader()) {
                body = br.lines().collect(Collectors.joining("\n"));
            }

            JsonObject jo = gson.fromJson(body, JsonObject.class);
            if (jo == null) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "empty json body")));
                return;
            }

            String patientId = getString(jo, "patientId");
            if (patientId == null || patientId.isBlank()) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "patientId is required")));
                return;
            }

            long minuteStartMs = getLong(jo, "minuteStartMs", -1);
            if (minuteStartMs <= 0) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "minuteStartMs must be > 0")));
                return;
            }

            double avgTemp = getDouble(jo, "avgTemp", 0);
            double avgHR   = getDouble(jo, "avgHR", 0);
            double avgRR   = getDouble(jo, "avgRR", 0);
            double avgSys  = getDouble(jo, "avgSys", 0);
            double avgDia  = getDouble(jo, "avgDia", 0);
            int sampleCount = getInt(jo, "sampleCount", 0);

            MinuteRecord r = new MinuteRecord(
                    minuteStartMs,
                    avgTemp,
                    avgHR,
                    avgRR,
                    avgSys,
                    avgDia,
                    sampleCount
            );

            dao.upsert(patientId, r);

            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":true}");

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("MINUTES_API_ERROR", e.getMessage())));
        }
    }

    // -------- helpers (copy style from VitalsIngestServlet) --------

    private static String getString(JsonObject jo, String k) {
        return (jo.has(k) && !jo.get(k).isJsonNull()) ? jo.get(k).getAsString() : null;
    }

    private static long getLong(JsonObject jo, String k, long def) {
        try {
            return (jo.has(k) && !jo.get(k).isJsonNull()) ? jo.get(k).getAsLong() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static double getDouble(JsonObject jo, String k, double def) {
        try {
            return (jo.has(k) && !jo.get(k).isJsonNull()) ? jo.get(k).getAsDouble() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int getInt(JsonObject jo, String k, int def) {
        try {
            return (jo.has(k) && !jo.get(k).isJsonNull()) ? jo.get(k).getAsInt() : def;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static final class ErrorMsg {
        final String code;
        final String message;

        ErrorMsg(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
