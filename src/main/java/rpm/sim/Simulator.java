package rpm.sim;

import rpm.model.VitalSample;

public interface Simulator {
    VitalSample nextSample(long nowMs);

    void setMode(SimulationMode mode);

    SimulationMode getMode();

    // tunable parameters (add more if you want)
    void setHeartRateBase(double bpm);

    double getHeartRateBase();
}

