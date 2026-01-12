package rpm.sim;

import rpm.model.VitalSample;
import rpm.model.Patient;
import java.util.Random;


public class SimpleVitalSimulator implements Simulator {


    private final Random rng = new Random();
    private SimulationMode mode = SimulationMode.NORMAL;

    private final Patient patient;

    private double hrBase = 75.0;
    private double tempBase = 36.7;
    private double rrBase = 14.0;
    private double sysBase = 120.0;
    private double diaBase = 80.0;

    private double phase = 0;


    public SimpleVitalSimulator(Patient patient) {
        this.patient = patient;
    }


    @Override
    public VitalSample nextSample(long nowMs) {
        // abnormal shift (demo)
        double abnormal = (mode == SimulationMode.ABNORMAL) ? 1.0 : 0.0;

        double hr = hrBase + 5 * Math.sin(phase) + noise(1.5) + abnormal * 35;
        double temp = tempBase + 0.05 * Math.sin(phase / 3) + noise(0.03) + abnormal * 1.5;
        double rr = rrBase + 1.5 * Math.sin(phase / 2) + noise(0.4) + abnormal * 10;
        double sys = sysBase + 8 * Math.sin(phase / 4) + noise(2.0) + abnormal * 40;
        double dia = diaBase + 5 * Math.sin(phase / 4) + noise(1.5) + abnormal * 20;

        double ecg = syntheticEcg(hr, phase);

        phase += 0.20;

        VitalSample sample = new VitalSample(nowMs, temp, hr, rr, sys, dia, ecg);

        return sample;
    }

    private double noise(double std) {
        return rng.nextGaussian() * std;
    }

    private double syntheticEcg(double hr, double p) {
        double beatsPerSec = hr / 60.0;
        double t = (p % (2 * Math.PI)) / (2 * Math.PI);  // 0..1
        // simple “spike”
        double spike = Math.exp(-Math.pow((t - 0.1) * beatsPerSec * 6, 2)) * 1.2;
        double baseline = 0.05 * Math.sin(p * 2) + noise(0.02);
        return baseline + spike;
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
}
