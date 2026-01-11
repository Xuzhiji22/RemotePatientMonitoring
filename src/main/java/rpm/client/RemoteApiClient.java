package rpm.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.io.OutputStream;


public class RemoteApiClient {

    private final String baseUrl;

    public RemoteApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
    }

    public String getPatientsJson() throws Exception {
        return get(baseUrl + "/api/patients");
    }

    public String getLatestVitalsJson(String patientId, int limit) throws Exception {
        return get(baseUrl + "/api/vitals/latest?patientId=" + patientId + "&limit=" + limit);
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

    public String postVitalSampleJson(String patientId,
                                      long timestampMs,
                                      double bodyTemp,
                                      double heartRate,
                                      double respiratoryRate,
                                      double systolicBP,
                                      double diastolicBP,
                                      double ecgValue) throws Exception {

        String json = "{"
                + "\"patientId\":\"" + escapeJson(patientId) + "\","
                + "\"timestampMs\":" + timestampMs + ","
                + "\"bodyTemp\":" + bodyTemp + ","
                + "\"heartRate\":" + heartRate + ","
                + "\"respiratoryRate\":" + respiratoryRate + ","
                + "\"systolicBP\":" + systolicBP + ","
                + "\"diastolicBP\":" + diastolicBP + ","
                + "\"ecgValue\":" + ecgValue
                + "}";

        return postJson(baseUrl + "/api/vitals", json);
    }

    private String postJson(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
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

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    public static void main(String[] args) throws Exception {
        RemoteApiClient client = new RemoteApiClient("https://bioeng-rpm-app.impaas.uk");

        System.out.println("patients=" + client.getPatientsJson());

        long ts = System.currentTimeMillis();
        String postResp = client.postVitalSampleJson(
                "P001",
                ts,
                36.8, 78.0, 15.0, 118.0, 79.0, 0.11
        );
        System.out.println("POST /api/vitals => " + postResp);

        System.out.println("vitals(P001)=" + client.getLatestVitalsJson("P001", 5));
    }

}
