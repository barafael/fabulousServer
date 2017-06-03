import java.util.Arrays;

/**
 * Created by jo on 02.06.17.
 */
public class Sensor{

    public int attr;
    public Integer[] array;

    public Sensor(int x, Integer[] a){
        this.attr = x;
        this.array = a;
    }

    public void setAttr(int c){
       this.attr=c;
    }

    public String printTheInt() {
        return Arrays.toString(this.array);
    }
}
