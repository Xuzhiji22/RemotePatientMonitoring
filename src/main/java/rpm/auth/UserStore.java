package rpm.auth;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserStore {
    private final Path file;
    private final Map<String, UserAccount> users = new LinkedHashMap<>();

    public UserStore(Path file) {
        this.file = file;
        load();
        if (users.isEmpty()) {
            // defaults
            users.put("admin",  new UserAccount("admin", "admin123", UserRole.ADMINISTRATOR));
            users.put("doctor", new UserAccount("doctor", "doctor123", UserRole.DOCTOR));
            users.put("nurse",  new UserAccount("nurse", "nurse123", UserRole.NURSE));
            try { save(); } catch (IOException ignored) {}
        }
    }

    public synchronized List<UserAccount> list() {
        return new ArrayList<>(users.values());
    }

    public synchronized Optional<UserAccount> authenticate(String u, String p) {
        UserAccount a = users.get(u);
        if (a == null) return Optional.empty();
        return a.password().equals(p) ? Optional.of(a) : Optional.empty();
    }

    public synchronized void upsert(UserAccount acc) {
        users.put(acc.username(), acc);
    }

    public synchronized void delete(String username) {
        users.remove(username);
    }

    public synchronized void save() throws IOException {
        Properties prop = new Properties();
        for (var e : users.entrySet()) {
            String u = e.getKey();
            UserAccount a = e.getValue();
            prop.setProperty(u + ".password", a.password());
            prop.setProperty(u + ".role", a.role().name());
        }
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            prop.store(out, "RPM user accounts");
        }
    }

    private synchronized void load() {
        if (!Files.exists(file)) return;
        Properties prop = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            prop.load(in);
            // parse by keys ending with .password
            for (String k : prop.stringPropertyNames()) {
                if (!k.endsWith(".password")) continue;
                String username = k.substring(0, k.length() - ".password".length());
                String pwd = prop.getProperty(username + ".password", "");
                String roleStr = prop.getProperty(username + ".role", "NURSE");
                UserRole role = UserRole.valueOf(roleStr);
                users.put(username, new UserAccount(username, pwd, role));
            }
        } catch (IOException ignored) { }
    }
}
