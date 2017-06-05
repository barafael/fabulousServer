package FHEMModel.timeserie;

/**
 * This class represents a single sample from a sensor in FHEM.
 * It is basically a Pair<Long, Double>
 * @author Rafael
 */

class Sample {
    private long timestamp;
    private double value;

    public Sample(long timestamp, double value) {
        assert timestamp >= 0;
        this.timestamp = timestamp;
        this.value = value;
    }

    boolean isNewerThan(Sample other) {
        return timestamp > other.timestamp;
    }
}
