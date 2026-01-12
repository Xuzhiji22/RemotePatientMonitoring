package rpm.web;

import rpm.dao.MinuteAverageDao;
import rpm.data.MinuteRecord;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/minutes/range"})
public class MinutesApiServlet extends HttpServlet {

    private final MinuteAverageDao dao = new MinuteAverageDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");

        String patientId = req.getParameter("patientId");
        String fromMsStr = req.getParameter("fromMs");
        String toMsStr = req.getParameter("toMs");

        if (patientId == null || fromMsStr == null || toMsStr == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"missing params: patientId, fromMs, toMs\"}");
            return;
        }

        long fromMs, toMs;
        try {
            fromMs = Long.parseLong(fromMsStr);
            toMs = Long.parseLong(toMsStr);
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"fromMs/toMs must be long\"}");
            return;
        }

        final List<MinuteRecord> list;
        try {
            list = dao.range(patientId, fromMs, toMs);
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"db error: " + escape(e.getMessage()) + "\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"patientId\":\"").append(escape(patientId)).append("\",");
        sb.append("\"fromMs\":").append(fromMs).append(",");
        sb.append("\"toMs\":").append(toMs).append(",");
        sb.append("\"minutes\":[");

        for (int i = 0; i < list.size(); i++) {
            MinuteRecord r = list.get(i);
            if (i > 0) sb.append(",");

            sb.append("{");
            sb.append("\"minuteStartMs\":").append(r.minuteStartMs()).append(",");
            sb.append("\"avgTemp\":").append(r.avgTemp()).append(",");
            sb.append("\"avgHr\":").append(r.avgHR()).append(",");
            sb.append("\"avgRr\":").append(r.avgRR()).append(",");
            sb.append("\"avgSys\":").append(r.avgSys()).append(",");
            sb.append("\"avgDia\":").append(r.avgDia()).append(",");
            sb.append("\"n\":").append(r.sampleCount());
            sb.append("}");
        }

        sb.append("]}");
        resp.getWriter().write(sb.toString());
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
