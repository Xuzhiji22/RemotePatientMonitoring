package rpm.sim;

import rpm.model.Patient;
import rpm.model.VitalSample;

import java.util.Random;

public class SimpleVitalSimulator implements Simulator {

    private final Random rng = new Random();
    private SimulationMode mode = SimulationMode.NORMAL;

    private final Patient patient;

    // Normal Base
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
        // 1. Calculate Drift Factor
        // Range: Oscillates between -1.0 and +1.0 roughly every few seconds.
        double wave = Math.sin(phase * 0.1);
        // If mode is NORMAL, abnormal factor is 0.0. If ABNORMAL, it is 1.0.
        double abnormal = (mode == SimulationMode.ABNORMAL) ? 1.0 : 0.0;

        // 2. Generate Vitals (Base + Periodic Variation + Random Noise + Abnormal Drift)
        // Heart Rate (HR)
        double hrShift = 45 + 40 * wave;
        double hr = hrBase + 5 * Math.sin(phase) + noise(1.5) + abnormal * hrShift;
        // Body Temperature (Temp)
        double tempShift = 1.5 + 1.2 * wave;
        double temp = tempBase + 0.05 * Math.sin(phase / 3) + noise(0.03) + abnormal * tempShift;
        // Respiratory Rate (RR)
        double rrShift = 12 + 10 * wave;
        double rr = rrBase + 1.5 * Math.sin(phase / 2) + noise(0.4) + abnormal * rrShift;
        // Systolic BP
        double sysShift = 40 + 35 * wave;
        double sys = sysBase + 8 * Math.sin(phase / 4) + noise(2.0) + abnormal * sysShift;
        // Diastolic BP
        double diaShift = 25 + 20 * wave;
        double dia = diaBase + 5 * Math.sin(phase / 4) + noise(1.5) + abnormal * diaShift;
        // Form ECG
        double ecg = syntheticEcg(hr, phase);

        // Create phase for the next sample
        phase += 0.20;

        return new VitalSample(nowMs, temp, hr, rr, sys, dia, ecg);
    }

    private double noise(double std) {
        return rng.nextGaussian() * std;
    }

    private double syntheticEcg(double hr, double p) {
        double t = (p % (2 * Math.PI)) / (2 * Math.PI);  // 0..1
        // simple P-QRS-T wave model
        double spike = Math.exp(-Math.pow((t - 0.5) * 20, 2));
        if (spike < 0.01) spike = 0;
        return spike * 2.0 - 0.5 + noise(0.05);
    }
}