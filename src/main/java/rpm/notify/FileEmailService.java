package rpm.notify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileEmailService implements EmailService {

    private final Path outDir;

    public FileEmailService(Path outDir) {
        this.outDir = outDir;
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create outbox dir: " + outDir, e);
        }
    }

    @Override
    public void send(String to, String subject, String body) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String safeTo = to.replaceAll("[^a-zA-Z0-9@._-]", "_");
        Path f = outDir.resolve(ts + "__to_" + safeTo + ".txt");

        String content =
                "TO: " + to + "\n" +
                        "SUBJECT: " + subject + "\n" +
                        "DATE: " + new Date() + "\n" +
                        "----------------------------------------\n" +
                        body + "\n";

        try {
            Files.writeString(f, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write email file: " + f, e);
        }
    }
}
