package rpm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigStore.
 * Tests reading and writing configuration properties to a temporary file.
 */
class ConfigStoreTest {

    // JUnit 5 will automatically create and clean up this temp directory
    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadConfig() throws IOException {
        // 1. Setup: Create a temporary config file path
        Path configFile = tempDir.resolve("system.properties");
        ConfigStore store = new ConfigStore(configFile);

        // Verify default config is returned when file is missing
        SystemConfig defaultConfig = store.loadOrDefault();
        assertNotNull(defaultConfig, "Should return default config if file missing");

        // 2. Action: Modify config and save using the new methods
        // Let's change update interval to 500ms and enable email alerts
        store.putInt("updateIntervalMs", 500);
        store.putBool("emailAlertsEnabled", true);

        // 3. Assertion: Reload from disk to verify persistence
        // Create a new store instance to read the same file to ensure persistence
        ConfigStore newStoreSession = new ConfigStore(configFile);
        SystemConfig loadedConfig = newStoreSession.loadOrDefault();

        assertEquals(500, loadedConfig.updateIntervalMs, "Should load saved integer value");
        assertTrue(loadedConfig.emailAlertsEnabled, "Should load saved boolean value");
    }

    @Test
    void testDefaultValues() {
        Path missingFile = tempDir.resolve("missing.properties");
        ConfigStore store = new ConfigStore(missingFile);

        SystemConfig cfg = store.loadOrDefault();

        // Default value in SystemConfig is 200ms
        assertEquals(200, cfg.updateIntervalMs, "Should return default value of 200");
    }
}