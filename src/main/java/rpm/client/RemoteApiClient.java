package rpm.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

    public static void main(String[] args) throws Exception {
        RemoteApiClient client = new RemoteApiClient("https://bioeng-rpm-app.impaas.uk");
        System.out.println("patients=" + client.getPatientsJson());
        System.out.println("vitals(P1)=" + client.getLatestVitalsJson("P123", 5));
    }
}
