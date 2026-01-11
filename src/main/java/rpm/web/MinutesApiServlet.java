package rpm.web;

import com.google.gson.Gson;
import rpm.dao.MinuteAverageDao;
import rpm.data.MinuteRecord;
import rpm.db.DbInit;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/api/minutes", "/api/minutes/latest"})
public class MinutesApiServlet extends HttpServlet {

    private final MinuteAverageDao dao = new MinuteAverageDao();
    private final Gson gson = new Gson();

    @Override
    public void init() {
        // 保证 minute_averages 表存在（避免必须先访问 /health 才创建表）
        try {
            DbInit.init();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        String path = req.getServletPath(); // "/api/minutes" or "/api/minutes/latest"
        String patientId = req.getParameter("patientId");

        if (patientId == null || patientId.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "patientId is required")));
            return;
        }

        try {
            if (path.endsWith("/latest")) {
                int limit = parseInt(req.getParameter("limit"), 60);
                List<MinuteRecord> out = dao.latest(patientId.trim(), limit);
                resp.getWriter().write(gson.toJson(out));
            } else {
                // /api/minutes?patientId=...&from=...&to=...
                long from = parseLong(req.getParameter("from"), -1);
                long to = parseLong(req.getParameter("to"), -1);

                if (from < 0 || to < 0) {
                    resp.setStatus(400);
                    resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "from and to are required (ms)")));
                    return;
                }

                List<MinuteRecord> out = dao.range(patientId.trim(), from, to);
                resp.getWriter().write(gson.toJson(out));
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("MINUTES_API_ERROR", e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        try {
            String body = readBody(req);
            Incoming in = gson.fromJson(body, Incoming.class);

            if (in == null || isBlank(in.patientId)) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "patientId is required")));
                return;
            }
            if (in.minuteStartMs <= 0) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "minuteStartMs is required (ms)")));
                return;
            }
            if (in.sampleCount < 0) {
                resp.setStatus(400);
                resp.getWriter().write(gson.toJson(new ErrorMsg("BAD_REQUEST", "sampleCount must be >= 0")));
                return;
            }

            MinuteRecord mr = new MinuteRecord(
                    in.minuteStartMs,
                    in.avgTemp,
                    in.avgHR,
                    in.avgRR,
                    in.avgSys,
                    in.avgDia,
                    in.sampleCount
            );

            dao.upsert(in.patientId.trim(), mr);
            resp.getWriter().write("{\"ok\":true}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write(gson.toJson(new ErrorMsg("MINUTES_API_ERROR", e.getMessage())));
        }
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (Exception ignored) { return def; }
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s); } catch (Exception ignored) { return def; }
    }

    static class Incoming {
        String patientId;
        long minuteStartMs;
        double avgTemp;
        double avgHR;
        double avgRR;
        double avgSys;
        double avgDia;
        int sampleCount;
    }

    static class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) { this.code = code; this.message = message; }
    }
}
