package rpm.auth;

public record UserAccount(String username, String password, UserRole role) {}
