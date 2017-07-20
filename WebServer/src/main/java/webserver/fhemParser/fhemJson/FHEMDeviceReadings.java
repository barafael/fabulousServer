package webserver.fhemParser.fhemJson;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents readings in jsonList2. It provides a method to get the fields which are actually set.
 *
 * @author Rafael on 04.07.17.
 */
final class FHEMDeviceReadings {
    /* Fields only written by Gson */
    private ValueTimePair battery;
    @SerializedName("battery_percent")
    private ValueTimePair batteryPercent;
    private ValueTimePair humidity;
    private ValueTimePair temperature;
    private ValueTimePair co2;
    private ValueTimePair power;
    private ValueTimePair voltage;
    private ValueTimePair current;

    /**
     * This method returns the readings' useful fields if set.
     * The implementation is very manual, but using Gson this is a simple way of doing it
     * while controlling which fields end up in the map.
     *
     * @return a map containing the set fields of the readings
     */
    Map<String, String> getReadings() {
        Map<String, String> ret = new HashMap<>();
        if (battery != null) {
            ret.put("Battery", battery.value);
        }
        if (batteryPercent != null) {
            ret.put("BatteryPercent", batteryPercent.value + " %");
        }
        if (humidity != null) {
            ret.put("Humidity", humidity.value + " %");
        }
        if (temperature != null) {
            ret.put("Temperature", temperature.value + " °C");
        }
        if (co2 != null) {
            ret.put("CO2", co2.value + " ppm");
        }
        if (power != null) {
            ret.put("Power", power.value + " W");
        }
        if (voltage != null) {
            ret.put("Voltage", voltage.value + " V");
        }
        if (current != null) {
            ret.put("Current", current.value + " mA");
        }
        return ret;
    }
}
