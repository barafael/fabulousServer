package WebServer.FHEMParser;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Rafael on 22.06.17.
 */

public class ModelSerializer implements JsonSerializer<FHEMModel> {
    private final List<String> permissions;

    public ModelSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public JsonElement serialize(FHEMModel model, Type type, JsonSerializationContext jsc) {
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions))
                .registerTypeAdapter(FHEMRoom.class, new RoomSerializer(permissions))
                .create()
                .toJsonTree(model);

        if (!model.hasPermittedRooms(permissions)) {
            return JsonNull.INSTANCE;
            // return null;
        }
        cleanNull(jObj);
        return jObj;
    }

    private void cleanNull(JsonElement element) {
        if (element.isJsonNull()) {
            System.err.println("JsonElement which is instance of JsonNull was not removed! " + element);
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
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
            HashSet<String> toDelete = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonNull()) {
                    toDelete.add(entry.getKey());
                } else {
                    cleanNull(entry.getValue());
                }
            }
            toDelete.forEach(jsonObject::remove);
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
        }
    }
}