package rpm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class ConfigStore {
    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    // Existing API: load/save SystemConfig

    public SystemConfig loadOrDefault() {
        SystemConfig cfg = new SystemConfig();
        Properties p = loadProperties();

        cfg.vitalsRetentionDays = intVal(p, "vitalsRetentionDays", cfg.vitalsRetentionDays);
        cfg.ecgRetentionDays = intVal(p, "ecgRetentionDays", cfg.ecgRetentionDays);
        cfg.alertsRetentionDays = intVal(p, "alertsRetentionDays", cfg.alertsRetentionDays);
        cfg.reportsRetentionDays = intVal(p, "reportsRetentionDays", cfg.reportsRetentionDays);

        cfg.emailAlertsEnabled = boolVal(p, "emailAlertsEnabled", cfg.emailAlertsEnabled);
        cfg.audioAlertsEnabled = boolVal(p, "audioAlertsEnabled", cfg.audioAlertsEnabled);

        cfg.updateIntervalMs = intVal(p, "updateIntervalMs", cfg.updateIntervalMs);
        cfg.sessionTimeoutMin = intVal(p, "sessionTimeoutMin", cfg.sessionTimeoutMin);
        cfg.logLevel = p.getProperty("logLevel", cfg.logLevel);

        return cfg;
    }

    /**
     * IMPORTANT:
     * This version preserves unknown keys in the properties file
     * (e.g. doctor.email, smtp.host, etc.)
     */
    public void save(SystemConfig cfg) throws IOException {
        Properties p = loadProperties();

        // overwrite known keys
        p.setProperty("vitalsRetentionDays", String.valueOf(cfg.vitalsRetentionDays));
        p.setProperty("ecgRetentionDays", String.valueOf(cfg.ecgRetentionDays));
        p.setProperty("alertsRetentionDays", String.valueOf(cfg.alertsRetentionDays));
        p.setProperty("reportsRetentionDays", String.valueOf(cfg.reportsRetentionDays));

        p.setProperty("emailAlertsEnabled", String.valueOf(cfg.emailAlertsEnabled));
        p.setProperty("audioAlertsEnabled", String.valueOf(cfg.audioAlertsEnabled));

        p.setProperty("updateIntervalMs", String.valueOf(cfg.updateIntervalMs));
        p.setProperty("sessionTimeoutMin", String.valueOf(cfg.sessionTimeoutMin));
        p.setProperty("logLevel", cfg.logLevel);

        storeProperties(p);
    }

    // ----------------------------
    // New API: generic get/put for extra keys
    // ----------------------------

    /** Returns Optional.empty() if missing or blank. */
    public Optional<String> getOptionalString(String key) {
        Properties p = loadProperties();
        String v = p.getProperty(key);
        if (v == null) return Optional.empty();
        v = v.trim();
        if (v.isEmpty()) return Optional.empty();
        return Optional.of(v);
    }

    public String getString(String key, String def) {
        return getOptionalString(key).orElse(def);
    }

    public int getInt(String key, int def) {
        Properties p = loadProperties();
        return intVal(p, key, def);
    }

    public boolean getBool(String key, boolean def) {
        Properties p = loadProperties();
        return boolVal(p, key, def);
    }

    /** Sets a key and persists immediately (handy for UI settings). */
    public void putString(String key, String value) throws IOException {
        Properties p = loadProperties();
        p.setProperty(key, value);
        storeProperties(p);
    }

    public void putInt(String key, int value) throws IOException {
        putString(key, String.valueOf(value));
    }

    public void putBool(String key, boolean value) throws IOException {
        putString(key, String.valueOf(value));
    }

    public void remove(String key) throws IOException {
        Properties p = loadProperties();
        p.remove(key);
        storeProperties(p);
    }

    // ----------------------------
    // Internals
    // ----------------------------

    private Properties loadProperties() {
        Properties p = new Properties();
        if (!Files.exists(file)) return p;

        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException ignored) { }
        return p;
    }

    private void storeProperties(Properties p) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "RPM system configuration");
        }
    }

    private int intVal(Properties p, String k, int def) {
        try { return Integer.parseInt(p.getProperty(k, String.valueOf(def)).trim()); }
        catch (Exception e) { return def; }
    }

    private boolean boolVal(Properties p, String k, boolean def) {
        try { return Boolean.parseBoolean(p.getProperty(k, String.valueOf(def)).trim()); }
        catch (Exception e) { return def; }
    }
}
