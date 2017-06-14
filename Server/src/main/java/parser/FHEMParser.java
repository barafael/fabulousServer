package parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import parser.fhemJson.JsonList2;
// TODO rethink design so singleton becomes unnecessary
/**
 * @author Rafael
 * @date 31.05.17.
 * Singleton class which provides a method 'parse' which takes a string and parses it to a jsonlist2 object
 * to parse all data associated with a FHEM Server given a fhemConnection.
 */

public class FHEMParser {
    /**
     * Static method to return the singleton instance of this parser
     */
    private static FHEMParser instance;
    /*
     * hide default constructor
     */
    private FHEMParser () {
    }

    public static synchronized FHEMParser getInstance() {
       if (FHEMParser.instance == null) {
           FHEMParser.instance = new FHEMParser();
       }
       return FHEMParser.instance;
    }

    public @NotNull JsonList2 parse(String jsonString) {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();

        return gson.fromJson(jsonString, JsonList2.class);
    }
}
