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
 * This serializer gets a list of permissions and, when serializing, uses them to filter out fields which should not be visible.
 * It is intended to be chained together with other serializers.
 *
 * @author Rafael on 22.06.17.
 */
class DoubleSerializer implements JsonSerializer<Double> {
    /**
     * Custom serializer for a Double, rounds a Double to three decimal places.
     *
     * @param dbl the source number
     * @return a JsonObject or jsonNull instance, depending on the permissions
     */
    @Override
    public JsonElement serialize(Double dbl, Type type, JsonSerializationContext jsc) {
        long factor = (long) Math.pow(10, 2);
        long tmp = Math.round(dbl * factor);
        double result = (double) tmp / factor;

        return new GsonBuilder()
                .create().toJsonTree(result);
    }
}
