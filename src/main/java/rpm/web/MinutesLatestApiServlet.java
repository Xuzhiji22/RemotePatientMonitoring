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

@WebServlet(urlPatterns = {"/api/minutes/latest"})
public class MinutesLatestApiServlet extends HttpServlet {

    private final MinuteAverageDao dao = new MinuteAverageDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");

        String patientId = req.getParameter("patientId");
        String limitStr  = req.getParameter("limit");

        if (patientId == null || limitStr == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"missing params: patientId, limit\"}");
            return;
        }

        int limit;
        try {
            limit = Integer.parseInt(limitStr);
            if (limit <= 0) limit = 60;
            if (limit > 1000) limit = 1000;
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"limit must be int\"}");
            return;
        }

        List<MinuteRecord> list;
        try {
            list = dao.latest(patientId, limit);
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"MinuteAverageDao.latest failed: " + escape(e.getMessage()) + "\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"patientId\":\"").append(escape(patientId)).append("\",");
        sb.append("\"limit\":").append(limit).append(",");
        sb.append("\"minutes\":[");

        for (int i = 0; i < list.size(); i++) {
            MinuteRecord m = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"minuteStartMs\":").append(m.minuteStartMs()).append(",");
            sb.append("\"avgTemp\":").append(m.avgTemp()).append(",");
            sb.append("\"avgHr\":").append(m.avgHR()).append(",");
            sb.append("\"avgRr\":").append(m.avgRR()).append(",");
            sb.append("\"avgSys\":").append(m.avgSys()).append(",");
            sb.append("\"avgDia\":").append(m.avgDia()).append(",");
            sb.append("\"n\":").append(m.sampleCount());
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
