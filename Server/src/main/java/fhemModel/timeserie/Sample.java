package fhemModel.timeserie;

/**
 * This class represents a single sample from a sensor in FHEM.
 * It is basically a Pair<String, Double>
 * @author Rafael
 */

class Sample {
    private final String date;
    private final double value;

    public String getDate() {
        return date;
    }

    public double getValue() {
        return value;
    }

    Sample(String date, double value) {
        this.date = date;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sample sample = (Sample) o;

        if (Double.compare(sample.value, value) != 0) return false;
        return date.equals(sample.date);
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
