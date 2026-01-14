package rpm.sim;

import rpm.model.Patient;
import rpm.model.VitalSample;

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


    private final long createdAt; // Simulator CreateTime
    private boolean isDestinedToGetSick = false;

    public SimpleVitalSimulator(Patient patient) {
        this.patient = patient;
        this.createdAt = System.currentTimeMillis();

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

        /*
        if (isDestinedToGetSick && mode == SimulationMode.NORMAL) {
            long elapsedSeconds = (nowMs - createdAt) / 1000;
            if (elapsedSeconds > 20) { // 20秒后自动发病
                //mode = SimulationMode.ABNORMAL;
                //System.out.println("⚠️ Patient " + patient.patientId() + " condition deteriorating!");
            }
        }
         */



        double wave = Math.sin(phase * 0.1);
        double abnormal = (mode == SimulationMode.ABNORMAL) ? 1.0 : 0.0;

        // Heart Rate (80 ~ 160)
        double hrShift = 45 + 40 * wave;
        double hr = hrBase + 5 * Math.sin(phase) + noise(1.5) + abnormal * hrShift;

        // Body Temperature (37.0 ~ 39.4)
        double tempShift = 1.5 + 1.2 * wave;
        double temp = tempBase + 0.05 * Math.sin(phase / 3) + noise(0.03) + abnormal * tempShift;

        // Respiratory (16 ~ 36)
        double rrShift = 12 + 10 * wave;
        double rr = rrBase + 1.5 * Math.sin(phase / 2) + noise(0.4) + abnormal * rrShift;

        // Blood Pressure
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