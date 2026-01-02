package rpm.config;

public class SystemConfig {
    public int vitalsRetentionDays = 365;
    public int ecgRetentionDays = 90;
    public int alertsRetentionDays = 180;
    public int reportsRetentionDays = 730;

    public boolean emailAlertsEnabled = true;
    public boolean audioAlertsEnabled = true;

    public int updateIntervalMs = 200;     // UI refresh
    public int sessionTimeoutMin = 30;

    public String logLevel = "INFO";       // DEBUG/INFO/WARN/ERROR
}
