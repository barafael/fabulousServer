package WebServer.FHEMParser.fhemUtils.serializers;

import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Rafael on 22.06.17.
 */
class FilelogSerializer implements JsonSerializer<FHEMFileLog> {
    private final List<String> permissions;

    FilelogSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Custom serializer for a filelog, only parses filelog if it is permitted.
     *
     * @param fileLog the source filelog
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
