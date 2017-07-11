package webserver.fhemParser.fhemJson;

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
    private ValueTimePair battery_percent;
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
            ret.put("battery", battery.value);
        }
        if (battery_percent != null) {
            ret.put("battery percent", battery_percent.value);
        }
        if (humidity != null) {
            ret.put("humidity", humidity.value);
        }
        if (temperature != null) {
            ret.put("temperature", temperature.value);
        }
        if (co2 != null) {
            ret.put("co2", co2.value);
        }
        if (power != null) {
            ret.put("power", power.value);
        }
        if (voltage != null) {
            ret.put("voltage", voltage.value);
        }
        if (current != null) {
            ret.put("current", current.value);
        }
        return ret;
    }
}
