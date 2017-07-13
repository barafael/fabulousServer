package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.stateCheck.rules.RuleInfo;

import java.lang.reflect.Type;
import java.util.List;

/**
 * This serializer gets a list of permissions and, when serializing, uses them to filter out fields which should not be visible.
 * It is intended to be chained together with other serializers.
 *
 * @author Rafael on 22.06.17.
 */
class RuleInfoSerializer implements JsonSerializer<RuleInfo> {
    /**
     * A list of permision identifiers that are used to remove/retain json elements.
     */
    private final List<String> permissions;

    RuleInfoSerializer(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Custom serializer for a ruleinfo, only parses ruleinfo if it is permitted.
     *
     * @param ruleInfo the source filelog
     *
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(RuleInfo ruleInfo, Type type, JsonSerializationContext jsc) {
        /* All lower serializers need to be reattached here since the custom serializer actually uses
        a separate instance of gson
         */
        JsonObject jObj = (JsonObject) new GsonBuilder()
                .create().toJsonTree(ruleInfo);
        if (!ruleInfo.isPermitted(permissions)) {
            return JsonNull.INSTANCE;
        }
        return jObj;
    }
}
