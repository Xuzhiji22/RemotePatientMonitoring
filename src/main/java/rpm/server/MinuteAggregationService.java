package rpm.server;

import rpm.alert.AlertEngine;
import rpm.dao.AbnormalEventDao;
import rpm.dao.MinuteAverageDao;
import rpm.dao.VitalSampleDao;
import rpm.data.AbnormalEvent;
import rpm.data.MinuteRecord;
import rpm.model.AlertLevel;
import rpm.model.VitalType;
import rpm.model.VitalSample;

import java.util.List;
import java.util.concurrent.*;

public final class MinuteAggregationService {

    private final VitalSampleDao vitalDao;
    private final MinuteAverageDao minuteDao;
    private final AbnormalEventDao abnormalDao;
    private final List<String> patientIds;

    // Reuse the same thresholds on server-side to produce abnormal instance records.
    private final AlertEngine alertEngine = new AlertEngine();

    private ScheduledExecutorService exec;

    public MinuteAggregationService(VitalSampleDao vitalDao, MinuteAverageDao minuteDao, List<String> patientIds) {
        this.vitalDao = vitalDao;
        this.minuteDao = minuteDao;
        this.abnormalDao = new AbnormalEventDao();
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
        long minuteStart = currentMinuteStart - 60000L;
        long minuteEnd = currentMinuteStart - 1;

        for (String pid : patientIds) {
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

            MinuteRecord r = new MinuteRecord(
                    minuteStart,
                    sumT / n,
                    sumHr / n,
                    sumRr / n,
                    sumSys / n,
                    sumDia / n,
                    n
            );

            try {
                minuteDao.upsert(pid, r);
            } catch (Exception ignored) {}

            persistAbnormalForMinute(pid, r);
        }
    }

    private void persistAbnormalForMinute(String patientId, MinuteRecord r) {
        long ts = r.minuteStartMs();

        recordIfAbnormal(patientId, ts, VitalType.BODY_TEMPERATURE, r.avgTemp(), "avgTemp");
        recordIfAbnormal(patientId, ts, VitalType.HEART_RATE,       r.avgHR(),   "avgHR");
        recordIfAbnormal(patientId, ts, VitalType.RESPIRATORY_RATE, r.avgRR(),   "avgRR");
        recordIfAbnormal(patientId, ts, VitalType.SYSTOLIC_BP,      r.avgSys(),  "avgSys");
        recordIfAbnormal(patientId, ts, VitalType.DIASTOLIC_BP,     r.avgDia(),  "avgDia");
    }

    private void recordIfAbnormal(String patientId, long ts, VitalType type, double value, String label) {
        AlertLevel level = alertEngine.eval(type, value);
        if (level == null || level == AlertLevel.NORMAL) return;

        String msg = label + " out of range (" + String.format("%.2f", value) + ")";

        try {
            abnormalDao.insert(patientId, new AbnormalEvent(ts, type, level, value, msg));
        } catch (Exception ignored) {
        }
    }
}
