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

    // ---------- GET ----------
    public String getPatientsJson() throws Exception {
        return get(baseUrl + "/api/patients");
    }

    public String getLatestVitalsJson(String patientId, int limit) throws Exception {
        return get(baseUrl + "/api/vitals/latest?patientId=" + patientId + "&limit=" + limit);
    }

    public String getLatestMinutesJson(String patientId, int limit) throws Exception {
        return get(baseUrl + "/api/minutes/latest?patientId=" + patientId + "&limit=" + limit);
    }

    // ---------- POST ----------
    public String postVitalSampleJson(String jsonBody) throws Exception {
        return postJson(baseUrl + "/api/vitals", jsonBody);
    }

    public String postMinuteAverageJson(String jsonBody) throws Exception {
        return postJson(baseUrl + "/api/minutes", jsonBody);
    }

    // ---------- low-level helpers ----------
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

    private String postJson(String urlStr, String jsonBody) throws Exception {
        byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(payload.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
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

    // ---------- quick smoke test ----------
    public static void main(String[] args) throws Exception {
        RemoteApiClient client = new RemoteApiClient("https://bioeng-rpm-app.impaas.uk");

        System.out.println("patients=" + client.getPatientsJson());


        long now = System.currentTimeMillis();
        String vitalBody =
                "{"
                        + "\"patientId\":\"P001\","
                        + "\"timestampMs\":" + now + ","
                        + "\"bodyTemp\":36.8,"
                        + "\"heartRate\":78.0,"
                        + "\"respiratoryRate\":15.0,"
                        + "\"systolicBP\":118.0,"
                        + "\"diastolicBP\":79.0,"
                        + "\"ecgValue\":0.11"
                        + "}";
        System.out.println("POST /api/vitals => " + client.postVitalSampleJson(vitalBody));
        System.out.println("vitals(P001)=" + client.getLatestVitalsJson("P001", 5));


        long minuteStart = (now / 60000L) * 60000L;
        String minuteBody =
                "{"
                        + "\"patientId\":\"P001\","
                        + "\"minuteStartMs\":" + minuteStart + ","
                        + "\"avgTemp\":36.8,"
                        + "\"avgHR\":78.0,"
                        + "\"avgRR\":15.0,"
                        + "\"avgSys\":118.0,"
                        + "\"avgDia\":79.0,"
                        + "\"sampleCount\":5"
                        + "}";
        System.out.println("POST /api/minutes => " + client.postMinuteAverageJson(minuteBody));
        System.out.println("minutes(P001)=" + client.getLatestMinutesJson("P001", 60));
    }
}
