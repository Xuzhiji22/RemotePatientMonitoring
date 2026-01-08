package rpm.auth;

import java.util.Objects;

public final class UserAccount {

    private final String username;
    private final String password;
    private final UserRole role;

    public UserAccount(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public UserRole role() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccount)) return false;

        UserAccount other = (UserAccount) o;
        return Objects.equals(username, other.username())
                && Objects.equals(password, other.password())
                && role == other.role();
    }


    @Override
    public int hashCode() {
        return Objects.hash(username, password, role);
    }

    @Override
    public String toString() {
        return "UserAccount[" +
                "username=" + username +
                ", password=" + password +
                ", role=" + role +
                "]";
    }
}
