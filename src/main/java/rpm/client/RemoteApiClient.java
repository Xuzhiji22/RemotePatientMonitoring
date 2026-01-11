package rpm.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RemoteApiClient {

    private final String baseUrl;

    public RemoteApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String getPatientsJson() throws Exception {
        return get(baseUrl + "/api/patients");
    }

    public String getLatestVitalsJson(String patientId, int limit) throws Exception {
        return get(baseUrl + "/api/vitals/latest?patientId=" + patientId + "&limit=" + limit);
    }

    public String postVitals(String jsonBody) throws Exception {
        return post(baseUrl + "/api/vitals", jsonBody);
    }

    public String getLatestMinutesJson(String patientId, int limit) throws Exception {
        return get(baseUrl + "/api/minutes/latest?patientId=" + patientId + "&limit=" + limit);
    }

    public String postMinutes(String jsonBody) throws Exception {
        return post(baseUrl + "/api/minutes", jsonBody);
    }

    public String getLatestAbnormalJson(String patientId, int limit) throws Exception {
        return get(baseUrl + "/api/abnormal/latest?patientId=" + patientId + "&limit=" + limit);
    }

    public String postAbnormal(String jsonBody) throws Exception {
        return post(baseUrl + "/api/abnormal", jsonBody);
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String post(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] out = jsonBody.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(out.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }

        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        RemoteApiClient client = new RemoteApiClient("https://bioeng-rpm-app.impaas.uk");

        String patientId = "P001";
        long now = System.currentTimeMillis();

        System.out.println("patients=" + client.getPatientsJson());

        // 1) post one vitals sample
        String vitalsBody =
                "{" +
                        "\"patientId\":\"" + patientId + "\"," +
                        "\"timestampMs\":" + now + "," +
                        "\"bodyTemp\":36.8," +
                        "\"heartRate\":78.0," +
                        "\"respiratoryRate\":15.0," +
                        "\"systolicBP\":118.0," +
                        "\"diastolicBP\":79.0," +
                        "\"ecgValue\":0.11" +
                        "}";
        System.out.println("POST /api/vitals => " + client.postVitals(vitalsBody));
        System.out.println("vitals(" + patientId + ")=" + client.getLatestVitalsJson(patientId, 5));

        // 2) post one minute average
        long minuteStart = (now / 60000L) * 60000L;
        String minutesBody =
                "{" +
                        "\"patientId\":\"" + patientId + "\"," +
                        "\"minuteStartMs\":" + minuteStart + "," +
                        "\"avgTemp\":36.8," +
                        "\"avgHR\":78.0," +
                        "\"avgRR\":15.0," +
                        "\"avgSys\":118.0," +
                        "\"avgDia\":79.0," +
                        "\"sampleCount\":5" +
                        "}";
        System.out.println("POST /api/minutes => " + client.postMinutes(minutesBody));
        System.out.println("minutes(" + patientId + ")=" + client.getLatestMinutesJson(patientId, 60));

        // 3) post one abnormal instance (example: high HR urgent)
        String abnormalBody =
                "{" +
                        "\"patientId\":\"" + patientId + "\"," +
                        "\"timestampMs\":" + now + "," +
                        "\"vitalType\":\"HR\"," +
                        "\"level\":\"URGENT\"," +
                        "\"value\":180.0," +
                        "\"message\":\"HR above urgent threshold\"" +
                        "}";
        System.out.println("POST /api/abnormal => " + client.postAbnormal(abnormalBody));
        System.out.println("abnormal(" + patientId + ")=" + client.getLatestAbnormalJson(patientId, 20));
    }
}
