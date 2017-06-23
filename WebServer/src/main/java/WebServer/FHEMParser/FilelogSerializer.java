package WebServer.FHEMParser;

import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Rafael on 22.06.17.
 */
public class FilelogSerializer implements JsonSerializer<FHEMFileLog> {
    private final List<String> permissions;

    public FilelogSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public JsonElement serialize(FHEMFileLog fileLog, Type type, JsonSerializationContext jsc) {
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .create().toJsonTree(fileLog);
        if (!fileLog.isPermitted(permissions)) {
            return JsonNull.INSTANCE;
            // return null;
        }
        System.out.println("test filelog " + fileLog + " against permissions here");
        return jObj;
    }
}
