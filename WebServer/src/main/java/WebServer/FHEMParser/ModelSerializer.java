package WebServer.FHEMParser;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rafael on 22.06.17.
 */

class ModelSerializer implements JsonSerializer<FHEMModel> {
    private final List<String> permissions;

    ModelSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Custom serializer for a model, only parses model if it is permitted.
     *
     * @param model the source model
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(FHEMModel model, Type type, JsonSerializationContext jsc) {
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions))
                .registerTypeAdapter(FHEMRoom.class, new RoomSerializer(permissions))
                .create()
                .toJsonTree(model);

        if (!model.hasPermittedRooms(permissions)) {
            return JsonNull.INSTANCE;
        }
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
            System.err.println("JsonElement which is instance of JsonNull was not removed!");
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            /* Hold all elements to be deleted (as to not invalidate the iterator)
             * This is reported as a false positive (I hope) by static analysis */
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
             * This is reported as a false positive (I hope) by static analysis */
            ArrayList<String> toDelete = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonNull()) {
                    toDelete.add(entry.getKey());
                } else {
                    cleanNull(entry.getValue());
                }
            }
            toDelete.forEach(jsonObject::remove);
        } /*else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
        } */
    }
}