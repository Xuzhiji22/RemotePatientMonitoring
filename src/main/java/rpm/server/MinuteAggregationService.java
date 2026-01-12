package rpm.server;

import rpm.dao.MinuteAverageDao;
import rpm.dao.VitalSampleDao;
import rpm.data.MinuteRecord;
import rpm.model.VitalSample;

import java.util.List;
import java.util.concurrent.*;

public final class MinuteAggregationService {

    private final VitalSampleDao vitalDao;
    private final MinuteAverageDao minuteDao;
    private final List<String> patientIds;

    private ScheduledExecutorService exec;

    public MinuteAggregationService(VitalSampleDao vitalDao, MinuteAverageDao minuteDao, List<String> patientIds) {
        this.vitalDao = vitalDao;
        this.minuteDao = minuteDao;
        this.patientIds = patientIds;
    }

    public void start() {
        if (exec != null) return;
        exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "minute-aggregator");
            t.setDaemon(true);
            return t;
        });

        exec.scheduleAtFixedRate(this::safeRun, 5, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (exec != null) exec.shutdownNow();
        exec = null;
    }

    private void safeRun() {
        try { aggregateLastFullMinute(); } catch (Exception ignored) {}
    }

    private void aggregateLastFullMinute() {
        long now = System.currentTimeMillis();
        long currentMinuteStart = (now / 60000L) * 60000L;
        long minuteStart = currentMinuteStart - 60000L;       // 上一分钟
        long minuteEnd = currentMinuteStart - 1;              // 上一分钟最后 1ms

        for (String pid : patientIds) {
            // 这里保持你现有 VitalSampleDao.range 的 4 参数签名： (pid, fromMs, toMs, limit)
            List<VitalSample> samples = vitalDao.range(pid, minuteStart, minuteEnd, 5000);
            if (samples == null || samples.isEmpty()) continue;

            double sumT=0, sumHr=0, sumRr=0, sumSys=0, sumDia=0;
            for (VitalSample s : samples) {
                sumT += s.bodyTemp();
                sumHr += s.heartRate();
                sumRr += s.respiratoryRate();
                sumSys += s.systolicBP();
                sumDia += s.diastolicBP();
            }
            int n = samples.size();

            // ✅ DB minute_averages 使用 MinuteRecord（data 层）
            MinuteRecord r = new MinuteRecord(
                    minuteStart,
                    sumT / n,
                    sumHr / n,
                    sumRr / n,
                    sumSys / n,
                    sumDia / n,
                    n
            );

            // ✅ MinuteAverageDao: upsert(String patientId, MinuteRecord r)
            try {
                minuteDao.upsert(pid, r);
            } catch (Exception ignored) {}
        }
    }
}
