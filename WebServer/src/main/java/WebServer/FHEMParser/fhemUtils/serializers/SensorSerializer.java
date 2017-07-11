package WebServer.FHEMParser.fhemUtils.serializers;

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

    SensorSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Custom serializer for sensor, only parses sensor if it is permitted.
     *
     * @param sensor the source sensor
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */

    @Override
    public JsonElement serialize(FHEMSensor sensor, Type type, JsonSerializationContext jsc) {
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .create().toJsonTree(sensor);
        if (!sensor.hasPermittedLogs(permissions)) {
            return JsonNull.INSTANCE;
        }
        return jObj;
    }
}