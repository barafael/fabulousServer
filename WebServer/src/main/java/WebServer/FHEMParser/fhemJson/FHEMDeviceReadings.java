package WebServer.FHEMParser.fhemJson;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafael on 04.07.17.
 */
class FHEMDeviceReadings {
    /* Fields only written by Gson */
    private ValueTimePair battery;
    private ValueTimePair battery_percent;
    private ValueTimePair humidity;
    private ValueTimePair temperature;
    private ValueTimePair co2;
    private ValueTimePair power;
    private ValueTimePair voltage;
    private ValueTimePair current;

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
