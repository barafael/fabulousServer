package WebServer.FHEMParser;

import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;

/**
 * @author Rafael on 22.06.17.
 */

import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

public class SensorSerializer implements JsonSerializer<FHEMSensor> {
    private final List<String> permissions;

    public SensorSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public JsonElement serialize(FHEMSensor sensor, Type type, JsonSerializationContext jsc) {
        JsonObject jObj = (JsonObject) new GsonBuilder().create().toJsonTree(sensor);
        if (!sensor.hasPermittedLogs(permissions)) {
            return null;
        }
        return jObj;
    }
}