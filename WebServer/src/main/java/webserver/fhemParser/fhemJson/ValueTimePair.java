package webserver.fhemParser.fhemJson;

import com.google.gson.annotations.SerializedName;

/**
 * This class represents a value-time pair in the FHEM readings.
 * It is necessary for deserialization for Gson to know the structure of the data in advance.
 *
 * @author Rafael on 04.07.17.
 */
class ValueTimePair {
    /* Those fields are only written by Gson */
    /**
     * A value from a reading.
     */
    @SerializedName("Value")
    protected String value;
    /* // Time; currently unused.
    @SerializedName("Time")
    private String time;
    */
}
