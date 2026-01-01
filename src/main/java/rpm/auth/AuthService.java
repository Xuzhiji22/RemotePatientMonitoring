package rpm.auth;

import java.util.List;
import java.util.Optional;

public class AuthService {

    // Hard-coded 3 accounts (demo)
    private final List<UserAccount> accounts = List.of(
            new UserAccount("admin", "admin123", UserRole.ADMINISTRATOR),
            new UserAccount("doctor", "doctor123", UserRole.DOCTOR),
            new UserAccount("nurse", "nurse123", UserRole.NURSE)
    );

    public Optional<UserAccount> authenticate(String username, String password) {
        return accounts.stream()
                .filter(a -> a.username().equals(username) && a.password().equals(password))
                .findFirst();
    }
}
