import java.util.Arrays;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 * some dummy sensor
 */
class Sensor {
    private int attr;

    private Integer[] array;

    public Sensor(int x, Integer[] a) {
        this.attr = x;
        this.array = a;
    }

    public void setAttr(int c) {
        this.attr = c;
    }

    public void setArray(Integer[] array) {
        this.array = array;
    }

    public String printTheInt() {
        return Arrays.toString(this.array);
    }
}
