package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.RuleEvent;
import webserver.ruleCheck.RuleSnapshot;
import webserver.ruleCheck.rules.RuleInfo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This custom serializer class for a FHEM model filters out all elements for which there are insufficient permissions.
 * The work is done in the private, recursive method
 * {@link webserver.fhemParser.fhemModel.serializers.ModelSerializer#cleanNull(JsonElement) cleanNull}.
 *
 * @author Rafael on 22.06.17.
 */
public final class ModelSerializer implements JsonSerializer<FHEMModel> {
    /**
     * A list of permission identifiers that are used to remove/retain json elements.
     */
    private final List<String> permissions;
    /**
     * The groups with which the model serializer should filter out events and snapshots.
     */
    private final List<String> groups;

    /**
     * Prevent direct construction without parameters.
     */
    private ModelSerializer() {
        permissions = new ArrayList<>();
        groups = new ArrayList<>();
    }

    /**
     * Construct this serializer, setting the permissions. Any model not permitted for them will be filtered out.
     *
     * @param permissions the permissions to use as filter
     * @param groups the groups to apply to sensors, history and snapshots
     */
    public ModelSerializer(List<String> permissions, List<String> groups) {
        this.permissions = permissions;
        this.groups = groups;
    }

    /**
     * Custom serializer for a model, only parses model if it is permitted.
     *
     * @param model the source model
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(FHEMModel model, Type type, JsonSerializationContext jsc) {
        if (!model.hasPermittedRooms(permissions)) {
            return JsonNull.INSTANCE;
        }
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        JsonObject jObj = (JsonObject) new GsonBuilder()
                /* Top-level attributes of the model should be filtered */
                .registerTypeAdapter(RuleEvent.class, new RuleEventSerializer(groups))
                .registerTypeAdapter(RuleSnapshot.class, new RuleSnapshotSerializer(groups))

                /* For filtering ruleinfo in sensors */
                .registerTypeAdapter(RuleInfo.class, new RuleInfoSerializer(groups))

                /* Elements of the FHEM model itself */
                /* Groups need to be passed so that the lower-level serializers can work with them */
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions, groups))
                .registerTypeAdapter(FHEMRoom.class, new RoomSerializer(permissions, groups))
                .create()
                .toJsonTree(model);

        cleanNull(jObj);

        return jObj;
    }

    /**
     * This method recursively traverses a json representation of a model, and removes all null elements
     * A JsonElement can be a JsonObject, a JsonArray, a JsonPrimitive, or a JsonNull.
     *
     * @param element the top element of the remaining tree
     */
    private void cleanNull(JsonElement element) {
        if (element.isJsonNull()) {
            System.err.println("JsonElement which is instance of JsonNull was not removed (or room was null)!");
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            /* Hold all elements to be deleted (as to not invalidate the iterator)
             * This is reported as a false positive by static analysis */
            ArrayList<JsonElement> toDelete = new ArrayList<>();
            for (JsonElement arr_elem : arr) {
                if (arr_elem.isJsonNull()) {
                    toDelete.add(arr_elem);
                } else {
                    cleanNull(arr_elem);
                }
            }
            toDelete.forEach(arr::remove);
        } else if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            /* Hold all elements to be deleted (as to not invalidate the iterator)
             * This is reported as a false positive by static analysis */
            ArrayList<String> toDelete = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonNull()) {
                    toDelete.add(entry.getKey());
                } else {
                    cleanNull(entry.getValue());
                }
            }
            toDelete.forEach(jsonObject::remove);
        } else //noinspection StatementWithEmptyBody
            if (element.isJsonPrimitive()) {
            /* JsonPrimitive prim = element.getAsJsonPrimitive(); */
            }
    }
}
