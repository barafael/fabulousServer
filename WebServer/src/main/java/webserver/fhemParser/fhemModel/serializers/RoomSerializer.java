package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.lang.reflect.Type;
import java.util.List;

/**
 * This serializer gets a list of permissions and, when  serializing, uses them to filter out fields which should not be visible.
 * It is intended to be chained together with other serializers.
 *
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
     *
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
