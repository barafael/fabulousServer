package server;

import parser.FHEMParser;

/**
 * Created by Rafael on 31.05.17.
 */
public class Server {

    private static FHEMParser parser = FHEMParser.getInstance();
    public static void main(String[] args) {
        String jsonString = "{\"Arg\":\"\", \"totalResultsReturned\":57}";

        parser.parse(jsonString);
    }
}
