package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;

import java.lang.reflect.Type;
import java.util.List;

/**
 * This serializer gets a list of permissions and, when  serializing, uses them to filter out fields which should not be visible.
 * It is intended to be chained together with other serializers.
 *
 * @author Rafael on 22.06.17.
 */
class FilelogSerializer implements JsonSerializer<FHEMFileLog> {
    /**
     * A list of permision identifiers that are used to remove/retain json elements.
     */
    private final List<String> permissions;

    FilelogSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Custom serializer for a filelog, only parses filelog if it is permitted.
     *
     * @param fileLog the source filelog
     *
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(FHEMFileLog fileLog, Type type, JsonSerializationContext jsc) {
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .create().toJsonTree(fileLog);
        if (!fileLog.isPermitted(permissions)) {
            return JsonNull.INSTANCE;
        }
        return jObj;
    }
}
