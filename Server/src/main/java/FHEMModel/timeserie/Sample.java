package FHEMModel.timeserie;

import java.time.LocalDateTime;

/**
 * This class represents a single sample from a sensor in FHEM.
 * It is basically a Pair<Long, Double>
 * @author Rafael
 */

class Sample {
    private final LocalDateTime date;
    private final double value;

    public LocalDateTime getDate() {
        return date;
    }

    public double getValue() {
        return value;
    }

    Sample(LocalDateTime date, double value) {
        this.date = date;
        this.value = value;
    }    @Override

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sample sample = (Sample) o;

        return Double.compare(sample.value, value) == 0 && date.equals(sample.date);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = date.hashCode();
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Sample{" +
                "date=" + date +
                ", value=" + value +
                '}';
    }
}
