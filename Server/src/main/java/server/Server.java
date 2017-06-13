package server;

import java.io.IOException;

import fhemConnection.FHEMConnection;
import fhemConnection.FHEMClientModeCon;
import com.google.gson.GsonBuilder;
import fhemModel.Model;
import com.google.gson.Gson;
import parser.FHEMParser;
import parser.fhemJson.JsonList2;

/**
 * @author Rafael on 31.05.17.
 */
class Server {

    private static final FHEMParser parser = FHEMParser.getInstance();

    public static void main(String[] args) {
        FHEMConnection fhc = new FHEMClientModeCon();
        String jsonList2 = "";
        try {
            jsonList2 = fhc.getJsonList2();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonList2 list = parser.parse(jsonList2);

        /* The other way around - just for testing.
        *  (Convert objects to json again and print)
        * */
        Model model = list.toFHEMModel();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(model));
    }
}
