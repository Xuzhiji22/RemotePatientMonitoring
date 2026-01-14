package rpm.sim;

import rpm.model.Patient;
import rpm.model.VitalSample;

import java.util.Random;

public class SimpleVitalSimulator implements Simulator {

    private final Random rng = new Random();
    private SimulationMode mode = SimulationMode.NORMAL;

    private final Patient patient;

    // 基础数值
    private double hrBase = 75.0;
    private double tempBase = 36.7;
    private double rrBase = 14.0;
    private double sysBase = 120.0;
    private double diaBase = 80.0;

    private double phase = 0;

    // --- 新增：自动恶化逻辑 ---
    private final long createdAt; // 模拟器创建时间
    private boolean isDestinedToGetSick = false; // 是否注定要生病

    public SimpleVitalSimulator(Patient patient) {
        this.patient = patient;
        this.createdAt = System.currentTimeMillis();

        // 设定：只有 ID 结尾是 '1' 或 '6' 的病人会变异 (例如 P001, P006)
        // 或者你可以根据名字判断: patient.name().endsWith("1")
        if (patient.patientId().endsWith("1") || patient.patientId().endsWith("6")) {
            this.isDestinedToGetSick = true;
        }
    }

    @Override
    public void setMode(SimulationMode mode) {
        this.mode = mode;
    }

    @Override
    public SimulationMode getMode() {
        return mode;
    }

    @Override
    public void setHeartRateBase(double bpm) {
        this.hrBase = bpm;
    }

    @Override
    public double getHeartRateBase() {
        return hrBase;
    }

    @Override
    public VitalSample nextSample(long nowMs) {
        // --- 1. 自动恶化检查 ---
        // 如果这个病人注定要生病，且当前是 NORMAL，且时间已经过了 20 秒
        /*
        if (isDestinedToGetSick && mode == SimulationMode.NORMAL) {
            long elapsedSeconds = (nowMs - createdAt) / 1000;
            if (elapsedSeconds > 20) { // 20秒后自动发病
                //mode = SimulationMode.ABNORMAL;
                //System.out.println("⚠️ Patient " + patient.patientId() + " condition deteriorating!");
            }
        }
         */

        // --- 2. 下面是之前写的正弦波漂移逻辑 (保持不变) ---

        double wave = Math.sin(phase * 0.1);
        double abnormal = (mode == SimulationMode.ABNORMAL) ? 1.0 : 0.0;

        // 心率 (80 ~ 160)
        double hrShift = 45 + 40 * wave;
        double hr = hrBase + 5 * Math.sin(phase) + noise(1.5) + abnormal * hrShift;

        // 体温 (37.0 ~ 39.4)
        double tempShift = 1.5 + 1.2 * wave;
        double temp = tempBase + 0.05 * Math.sin(phase / 3) + noise(0.03) + abnormal * tempShift;

        // 呼吸 (16 ~ 36)
        double rrShift = 12 + 10 * wave;
        double rr = rrBase + 1.5 * Math.sin(phase / 2) + noise(0.4) + abnormal * rrShift;

        // 血压
        double sysShift = 40 + 35 * wave;
        double sys = sysBase + 8 * Math.sin(phase / 4) + noise(2.0) + abnormal * sysShift;

        double diaShift = 25 + 20 * wave;
        double dia = diaBase + 5 * Math.sin(phase / 4) + noise(1.5) + abnormal * diaShift;

        double ecg = syntheticEcg(hr, phase);

        phase += 0.20;

        return new VitalSample(nowMs, temp, hr, rr, sys, dia, ecg);
    }

    private double noise(double std) {
        return rng.nextGaussian() * std;
    }

    private double syntheticEcg(double hr, double p) {
        double t = (p % (2 * Math.PI)) / (2 * Math.PI);
        double spike = Math.exp(-Math.pow((t - 0.5) * 20, 2));
        if (spike < 0.01) spike = 0;
        return spike * 2.0 - 0.5 + noise(0.05);
    }
}