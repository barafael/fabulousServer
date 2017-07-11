package WebServer.FHEMParser.fhemUtils.serializers;

import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Rafael on 22.06.17.
 */

class RoomSerializer implements JsonSerializer<FHEMRoom> {
    private final List<String> permissions;

    RoomSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Custom serializer for room, only parses room if it is permitted.
     *
     * @param room the source room
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */

    @Override
    public JsonElement serialize(FHEMRoom room, Type type, JsonSerializationContext jsc) {
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions))
                .create().toJsonTree(room);
        if (!room.hasPermittedSensors(permissions)) {
            return JsonNull.INSTANCE;
        }
        return jObj;
    }
}