package rpm.data;

import rpm.model.VitalSample;

import java.util.List;

public class PatientDataStore {
    private final RingBuffer<VitalSample> samples;

    public PatientDataStore(int maxSeconds, int sampleHz) {
        int capacity = Math.max(1, maxSeconds * sampleHz);
        this.samples = new RingBuffer<>(capacity);
    }

    public void addSample(VitalSample s) {
        samples.add(s);
    }

    public List<VitalSample> getBufferedSamples() {
        return samples.snapshot();
    }
}
