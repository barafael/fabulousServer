package webserver.fhemParser.fhemModel.serializers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * This serializer gets a list of permissions and, when serializing, uses them to filter out
 * fields which should not be visible.
 * It is intended to be chained together with other serializers.
 *
 * @author Rafael on 22.06.17.
 */
public class DoubleSerializer implements JsonSerializer<Double> {
    /**
     * Custom serializer for a Double, rounds a Double to two decimal places.
     *
     * @param dbl the source number
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(Double dbl, Type type, JsonSerializationContext jsc) {
        long tmp = Math.round(dbl * 100);
        double result = (double) tmp / 100;
        return new Gson().toJsonTree(result);
    }
}
