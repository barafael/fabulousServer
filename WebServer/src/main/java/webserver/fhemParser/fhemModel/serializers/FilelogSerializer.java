package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;

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
final class FilelogSerializer implements JsonSerializer<FHEMFileLog> {
    /**
     * A list of permission identifiers that are used to remove/retain json elements.
     */
    private final List<String> permissions;

    /**
     * Prevent direct construction without parameters.
     */
    private FilelogSerializer() {
        permissions = new ArrayList<>();
    }

    /**
     * Construct this serializer, setting the permissions. Any filelog not permitted for them will be filtered out.
     *
     * @param permissions the permissions to use as filter
     */
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
        if (!fileLog.isPermitted(permissions)) {
            return JsonNull.INSTANCE;
        }
        return new Gson().toJsonTree(fileLog);
    }
}
