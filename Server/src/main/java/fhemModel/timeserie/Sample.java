package fhemModel.timeserie;

/**
 * This class represents a single sample from a sensor in FHEM.
 * It is basically a Pair<Long, T>
 * @author Rafael
 */

class Sample<T extends Number> {
    private final long timestamp;
    private final T value;

    public Sample(long epoch, T value) {
        timestamp = epoch;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sample<?> sample = (Sample<?>) o;

        return timestamp == sample.timestamp && value.equals(sample.value);
    }

    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Sample{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
