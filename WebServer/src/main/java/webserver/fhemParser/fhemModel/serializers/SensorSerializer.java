package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.rules.RuleInfo;

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
class SensorSerializer implements JsonSerializer<FHEMSensor> {
    /**
     * A list of permission identifiers that are used to remove/retain json elements.
     */
    private final List<String> permissions;

    /**
     * Prevent direct construction without parameters.
     */
    private SensorSerializer() {
        permissions = new ArrayList<>();
    }

    /**
     * Construct this serializer, setting the permissions. Any sensor not permitted for them will be filtered out.
     *
     * @param permissions the permissions to use as filter
     */
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
                .registerTypeAdapter(RuleInfo.class, new RuleInfoSerializer(permissions))
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .create().toJsonTree(sensor);
        if (sensor.hasPermittedLogs(permissions) || sensor.hasPermittedRules(permissions)) {
            return jObj;
        }
        return JsonNull.INSTANCE;
    }
}
