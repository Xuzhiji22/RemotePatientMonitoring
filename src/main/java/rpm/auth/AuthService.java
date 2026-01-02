package rpm.auth;

import java.util.Optional;

public class AuthService {
    private final UserStore store;

    public AuthService(UserStore store) {
        this.store = store;
    }

    public Optional<UserAccount> authenticate(String username, String password) {
        return store.authenticate(username, password);
    }
}
