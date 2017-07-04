package WebServer.FHEMParser.fhemJson;

import com.google.gson.annotations.SerializedName;

/**
 * @author Rafael on 04.07.17.
 */
public class ValueTimePair {
    @SerializedName("Value")
    String value;
    @SerializedName("Time")
    String time;

    @Override
    public String toString() {
        return "ValueTimePair{" +
                "value='" + value + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
