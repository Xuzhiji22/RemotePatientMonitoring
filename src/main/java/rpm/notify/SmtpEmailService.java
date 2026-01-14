package rpm.notify;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmtpEmailService implements EmailService {

    private final Session session;
    private final String from;

    public SmtpEmailService(
            String host,
            int port,
            String username,
            String password,
            boolean useStartTls,
            String from
    ) {
        this.from = from;

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(useStartTls));

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to, false)
            );
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }
}
