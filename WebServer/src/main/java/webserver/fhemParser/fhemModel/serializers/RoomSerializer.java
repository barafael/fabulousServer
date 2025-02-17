package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * This serializer gets a list of permissions and, when serializing, uses them to filter out
 * fields which should not be visible.
 * It is intended to be chained together with other serializers.
 *
 * @author Rafael on 22.06.17.
 */
final class RoomSerializer implements JsonSerializer<FHEMRoom> {
    /**
     * A list of permission identifiers that are used to remove/retain json elements.
     */
    private final List<String> permissions;

    /**
     * A list of group identifiers that are used to remove/retain ruleinfo/ruleevent/rulesnapshots.
     */
    private final List<String> groups;

    /**
     * Prevent direct construction without parameters.
     */
    private RoomSerializer() {
        permissions = new ArrayList<>();
        groups = new ArrayList<>();
    }

    /**
     * Construct this serializer, setting the permissions. Any room not permitted for them will be filtered out.
     *
     * @param permissions the permissions to use as filter
     * @param groups      the groups needed to view this sensor
     */
    RoomSerializer(List<String> permissions, List<String> groups) {
        this.permissions = permissions;
        this.groups = groups;
    }

    /**
     * Custom serializer for room, only parses room if it is permitted.
     *
     * @param room the source room
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(FHEMRoom room, Type type, JsonSerializationContext jsc) {
        if (!room.hasPermittedSensors(permissions)) {
            return JsonNull.INSTANCE;
        }
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        return new GsonBuilder()
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions, groups))
                .create().toJsonTree(room);
    }
}
