package WebServer.FHEMParser;

import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Rafael on 22.06.17.
 */

class SensorSerializer implements JsonSerializer<FHEMSensor> {
    private final List<String> permissions;

    public SensorSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public JsonElement serialize(FHEMSensor sensor, Type type, JsonSerializationContext jsc) {
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .create().toJsonTree(sensor);
        if (!sensor.hasPermittedLogs(permissions)) {
            return JsonNull.INSTANCE;
            // return null;
        }
        return jObj;
    }
}