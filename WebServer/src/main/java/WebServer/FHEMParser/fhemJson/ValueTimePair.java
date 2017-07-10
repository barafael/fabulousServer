package WebServer.FHEMParser.fhemJson;

import com.google.gson.annotations.SerializedName;

/**
 * This class represents a value-time pair in the FHEM readings.
 * It is necessary for deserialization for Gson to know the structure of the data in advance.
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
