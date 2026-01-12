package rpm.web;

import rpm.dao.VitalSampleDao;
import rpm.model.VitalSample;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/vitals/*"})
public class VitalsApiServlet extends HttpServlet {

    private final VitalSampleDao dao = new VitalSampleDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");

        String path = req.getPathInfo(); // e.g. "/latest" or "/range"
        if (path == null) path = "/latest";

        try {
            if ("/latest".equals(path)) {
                handleLatest(req, resp);
                return;
            }

            if ("/range".equals(path)) {
                handleRange(req, resp);
                return;
            }

            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"Unknown endpoint. Use /latest or /range\"}");

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleLatest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String patientId = req.getParameter("patientId");
        if (patientId == null || patientId.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"missing patientId\"}");
            return;
        }

        // 用 range(过去10秒) 来近似 latest：取最后一个点（避免你现有 DAO 没有 getLatest）
        long now = System.currentTimeMillis();
        List<VitalSample> samples = dao.range(patientId, now - 10_000, now, 1);
        if (samples.isEmpty()) {
            resp.getWriter().write("{\"patientId\":\"" + escape(patientId) + "\",\"sample\":null}");
            return;
        }

        VitalSample s = samples.get(samples.size() - 1);
        resp.getWriter().write(sampleJson(patientId, s));
    }

    private void handleRange(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String patientId = req.getParameter("patientId");
        String fromMsStr = req.getParameter("fromMs");
        String toMsStr = req.getParameter("toMs");

        if (patientId == null || patientId.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"missing patientId\"}");
            return;
        }
        if (fromMsStr == null || toMsStr == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"missing fromMs/toMs\"}");
            return;
        }

        long fromMs = Long.parseLong(fromMsStr);
        long toMs = Long.parseLong(toMsStr);

        List<VitalSample> samples = dao.range(patientId, fromMs, toMs, 1000);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"patientId\":\"").append(escape(patientId)).append("\",");
        sb.append("\"fromMs\":").append(fromMs).append(",");
        sb.append("\"toMs\":").append(toMs).append(",");
        sb.append("\"samples\":[");

        for (int i = 0; i < samples.size(); i++) {
            VitalSample s = samples.get(i);
            sb.append("{")
                    .append("\"tsMs\":").append(s.timestampMs()).append(",")
                    .append("\"temp\":").append(s.bodyTemp()).append(",")
                    .append("\"hr\":").append(s.heartRate()).append(",")
                    .append("\"rr\":").append(s.respiratoryRate()).append(",")
                    .append("\"sys\":").append(s.systolicBP()).append(",")
                    .append("\"dia\":").append(s.diastolicBP()).append(",")
                    .append("\"ecg\":").append(s.ecgValue())
                    .append("}");
            if (i < samples.size() - 1) sb.append(",");
        }

        sb.append("]}");
        resp.getWriter().write(sb.toString());
    }

    private String sampleJson(String patientId, VitalSample s) {
        return "{"
                + "\"patientId\":\"" + escape(patientId) + "\","
                + "\"sample\":{"
                + "\"tsMs\":" + s.timestampMs() + ","
                + "\"temp\":" + s.bodyTemp() + ","
                + "\"hr\":" + s.heartRate() + ","
                + "\"rr\":" + s.respiratoryRate() + ","
                + "\"sys\":" + s.systolicBP() + ","
                + "\"dia\":" + s.diastolicBP() + ","
                + "\"ecg\":" + s.ecgValue()
                + "}"
                + "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
