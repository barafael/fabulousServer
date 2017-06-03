import java.util.Arrays;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 * some dummy sensor
 */
public class Sensor {

    public int attr;
    public Integer[] array;

    public Sensor(int x, Integer[] a) {
        this.attr = x;
        this.array = a;
    }

    public void setAttr(int c) {
        this.attr = c;
    }

    public String printTheInt() {
        return Arrays.toString(this.array);
    }
}
