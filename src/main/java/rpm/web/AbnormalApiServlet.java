package rpm.web;

import com.google.gson.Gson;
import rpm.dao.AbnormalEventDao;
import rpm.data.AbnormalEvent;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/api/abnormal/latest", "/api/abnormal"})
public class AbnormalApiServlet extends HttpServlet {

    private final AbnormalEventDao abnormalDao = new AbnormalEventDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // GET /api/abnormal/latest?patientId=...&limit=...
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
            List<AbnormalEvent> events = abnormalDao.latest(patientId.trim(), limit);
            resp.getWriter().write(gson.toJson(events));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("ABNORMAL_API_ERROR", e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // POST /api/abnormal  body: { patientId, timestampMs, vitalType, level, value, message }
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        try {
            PostMsg msg = readJson(req, PostMsg.class);
            if (msg == null || msg.patientId == null || msg.patientId.isBlank()) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "patientId is required")));
                return;
            }

            AbnormalEvent e = new AbnormalEvent(
                    msg.timestampMs,
                    rpm.model.VitalType.valueOf(msg.vitalType),
                    rpm.model.AlertLevel.valueOf(msg.level),
                    msg.value,
                    msg.message
            );

            abnormalDao.insert(msg.patientId.trim(), e);

            resp.getWriter().write(gson.toJson(new OkMsg(true)));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("ABNORMAL_INGEST_ERROR", e.getMessage())));
        }
    }

    private <T> T readJson(HttpServletRequest req, Class<T> cls) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String body = sb.toString().trim();
            if (body.isEmpty()) return null;
            return gson.fromJson(body, cls);
        }
    }

    static final class PostMsg {
        String patientId;
        long timestampMs;
        String vitalType;
        String level;
        double value;
        String message;
    }

    static final class OkMsg {
        boolean ok;
        OkMsg(boolean ok) { this.ok = ok; }
    }

    static final class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) { this.code = code; this.message = message; }
    }
}
