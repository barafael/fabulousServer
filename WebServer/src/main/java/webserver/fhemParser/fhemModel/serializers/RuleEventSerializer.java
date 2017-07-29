package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import webserver.ruleCheck.RuleEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rafael on 27.07.17.
 */
public final class RuleEventSerializer implements JsonSerializer<RuleEvent> {
    /**
     * A list of permission identifiers that are used to remove/retain json elements.
     */
    private final List<String> groups;

    /**
     * Prevent direct construction without parameters.
     */
    private RuleEventSerializer() {
        groups = new ArrayList<>();
    }

    /**
     * Construct this serializer, setting the permissions. Any model not permitted for them will be filtered out.
     *
     * @param groups the permissions to use as filter
     */
    public RuleEventSerializer(List<String> groups) {
        this.groups = groups;
    }

    /**
     * Custom serializer for a event, only parses event if it is permitted.
     *
     * @param event the source event
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(RuleEvent event, Type type, JsonSerializationContext jsc) {
        if (!event.isPermittedForGroups(groups)) {
            return JsonNull.INSTANCE;
        }
        return new Gson().toJsonTree(event);
    }
}
