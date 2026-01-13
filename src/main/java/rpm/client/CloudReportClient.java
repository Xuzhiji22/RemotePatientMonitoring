package rpm.client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CloudReportClient {
    private final String baseUrl;

    public CloudReportClient(String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    public String getLatestMinutesJson(String patientId, int limit) throws Exception {
        return httpGet(baseUrl + "/api/minutes/latest?patientId=" + patientId + "&limit=" + limit);
    }

    public String getLatestAbnormalJson(String patientId, int limit) throws Exception {
        return httpGet(baseUrl + "/api/abnormal/latest?patientId=" + patientId + "&limit=" + limit);
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.setRequestMethod("GET");

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String body = readAll(is);

        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + " body=" + body);
        }
        return body;
    }


    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return null;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
