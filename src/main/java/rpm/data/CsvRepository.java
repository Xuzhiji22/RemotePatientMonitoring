package rpm.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class CsvRepository {

    private final Path baseDir;

    public CsvRepository(Path baseDir) {
        this.baseDir = baseDir;
    }

    public void appendMinuteRecord(String patientId, MinuteRecord rec) throws IOException {
        Files.createDirectories(baseDir);

        Path file = baseDir.resolve(patientId + "_minute_records.csv");
        boolean newFile = !Files.exists(file);

        String header = "minuteStartIso,avgTemp,avgHR,avgRR,avgSys,avgDia,sampleCount\n";
        String line = String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%d%n",
                Instant.ofEpochMilli(rec.minuteStartMs()),
                rec.avgTemp(), rec.avgHR(), rec.avgRR(), rec.avgSys(), rec.avgDia(),
                rec.sampleCount());

        if (newFile) {
            Files.writeString(file, header, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void appendAbnormalEvent(String patientId, AbnormalEvent ev) throws IOException {
        Files.createDirectories(baseDir);

        Path file = baseDir.resolve(patientId + "_abnormal_events.csv");
        boolean newFile = !Files.exists(file);

        String header = "timestampIso,vitalType,level,value,message\n";
        String line = String.format("%s,%s,%s,%.4f,%s%n",
                Instant.ofEpochMilli(ev.timestampMs()),
                ev.vitalType(),
                ev.level(),
                ev.value(),
                escape(ev.message()));

        if (newFile) {
            Files.writeString(file, header, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private String escape(String s) {
        if (s == null) return "";
        // simplest CSV escaping
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\n")) return "\"" + t + "\"";
        return t;
    }
}
