package rpm.notify;

public interface EmailService {
    void send(String to, String subject, String body);
}
