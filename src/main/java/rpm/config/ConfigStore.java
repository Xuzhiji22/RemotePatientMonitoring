package rpm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigStore {
    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    public SystemConfig loadOrDefault() {
        SystemConfig cfg = new SystemConfig();
        if (!Files.exists(file)) return cfg;

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
            cfg.vitalsRetentionDays = intVal(p, "vitalsRetentionDays", cfg.vitalsRetentionDays);
            cfg.ecgRetentionDays = intVal(p, "ecgRetentionDays", cfg.ecgRetentionDays);
            cfg.alertsRetentionDays = intVal(p, "alertsRetentionDays", cfg.alertsRetentionDays);
            cfg.reportsRetentionDays = intVal(p, "reportsRetentionDays", cfg.reportsRetentionDays);

            cfg.emailAlertsEnabled = boolVal(p, "emailAlertsEnabled", cfg.emailAlertsEnabled);
            cfg.audioAlertsEnabled = boolVal(p, "audioAlertsEnabled", cfg.audioAlertsEnabled);

            cfg.updateIntervalMs = intVal(p, "updateIntervalMs", cfg.updateIntervalMs);
            cfg.sessionTimeoutMin = intVal(p, "sessionTimeoutMin", cfg.sessionTimeoutMin);
            cfg.logLevel = p.getProperty("logLevel", cfg.logLevel);

        } catch (IOException ignored) { }
        return cfg;
    }

    public void save(SystemConfig cfg) throws IOException {
        Properties p = new Properties();
        p.setProperty("vitalsRetentionDays", String.valueOf(cfg.vitalsRetentionDays));
        p.setProperty("ecgRetentionDays", String.valueOf(cfg.ecgRetentionDays));
        p.setProperty("alertsRetentionDays", String.valueOf(cfg.alertsRetentionDays));
        p.setProperty("reportsRetentionDays", String.valueOf(cfg.reportsRetentionDays));

        p.setProperty("emailAlertsEnabled", String.valueOf(cfg.emailAlertsEnabled));
        p.setProperty("audioAlertsEnabled", String.valueOf(cfg.audioAlertsEnabled));

        p.setProperty("updateIntervalMs", String.valueOf(cfg.updateIntervalMs));
        p.setProperty("sessionTimeoutMin", String.valueOf(cfg.sessionTimeoutMin));
        p.setProperty("logLevel", cfg.logLevel);

        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "RPM system configuration");
        }
    }

    private int intVal(Properties p, String k, int def) {
        try { return Integer.parseInt(p.getProperty(k, String.valueOf(def)).trim()); }
        catch (Exception e) { return def; }
    }

    private boolean boolVal(Properties p, String k, boolean def) {
        return Boolean.parseBoolean(p.getProperty(k, String.valueOf(def)).trim());
    }
}
