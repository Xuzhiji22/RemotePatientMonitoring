package rpm.web;

import com.google.gson.Gson;
import rpm.dao.MinuteAverageDao;
import rpm.data.MinuteRecord;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/minutes", "/api/minutes/latest"})
public class MinutesApiServlet extends HttpServlet {

    private final MinuteAverageDao dao = new MinuteAverageDao();
    private final Gson gson = new Gson();

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

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (Exception ignored) { return def; }
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s); } catch (Exception ignored) { return def; }
    }

    static class ErrorMsg {
        String code;
        String message;
        ErrorMsg(String code, String message) { this.code = code; this.message = message; }
    }
}
