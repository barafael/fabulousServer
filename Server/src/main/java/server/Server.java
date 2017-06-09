package server;

import java.io.IOException;

import FHEMConnection.FHEMConnection;
import FHEMConnection.FHEMClientModeCon;
import FHEMModel.Model;
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
            jsonList2 = fhc.getJsonList2(7072, "/opt/fhem/fhem.pl");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonList2 list = parser.parse(jsonList2);

        /* The other way around - just for testing.
        *  (Convert objects to json again and print)
        * */
        Gson gson = new Gson();
        System.out.println(gson.toJson(list));
        Model model = list.toFHEMModel();
    }
}
