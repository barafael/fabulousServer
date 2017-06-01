package parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import parser.fhemJson.JsonList2;

/**
 * Created by Rafael on 31.05.17.
 * Singleton class which provides a method //TODO which
 * to parse all data associated with a FHEM Server given a FHEMConnection.
 */
public class FHEMParser {
    /**
     * static method to return the singleton instance of this parser
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

    public JsonList2 parse(String jsonString) {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();
        JsonList2 list = gson.fromJson(jsonString, JsonList2.class);

        return list;

    }
}
