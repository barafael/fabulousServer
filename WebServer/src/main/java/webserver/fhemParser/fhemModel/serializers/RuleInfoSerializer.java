package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
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
class RuleInfoSerializer implements JsonSerializer<RuleInfo> {
    /**
     * A list of group identifiers that are used to remove/retain json elements.
     */
    private final List<String> groups;

    /**
     * Prevent direct construction without parameters.
     */
    private RuleInfoSerializer() {
        groups = new ArrayList<>();
    }

    /**
     * Construct this serializer, setting the permissions. Any RuleInfo not permitted for them will be filtered out.
     *
     * @param groups the permissions to use as filter
     */
    RuleInfoSerializer(List<String> groups) {
        this.groups = groups;
    }

    /**
     * Custom serializer for a ruleinfo, only parses ruleinfo if it is permitted.
     *
     * @param ruleInfo the source filelog
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(RuleInfo ruleInfo, Type type, JsonSerializationContext jsc) {
        if (!ruleInfo.isPermittedForGroups(groups)) {
            return JsonNull.INSTANCE;
        }
        return new Gson().toJsonTree(ruleInfo);
    }
}
