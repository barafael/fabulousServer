package server;

import java.io.IOException;

import fhemConnection.FHEMConnection;
import fhemConnection.FHEMClientModeCon;
import com.google.gson.GsonBuilder;
import fhemConnection.FHEMNotFoundException;
import com.google.gson.Gson;
import fhemModel.Model;
import parser.FHEMParser;
import parser.fhemJson.JsonList2;

/**
 * @author Rafael on 31.05.17.
 */
class Server {

    private static final FHEMParser parser = FHEMParser.getInstance();

    public static void main(String[] args) {
        FHEMConnection fhc = new FHEMClientModeCon();
        try {
            String jsonList2_str = fhc.getJsonList2();
            JsonList2 list = parser.parse(jsonList2_str);

            Model model = list.toFHEMModel();
            /* The other way around - just for testing.
            *  (Convert objects to json again and print)
            * */
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(model));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FHEMNotFoundException fnfe) {
            System.err.println("FHEM might not be running.");
            System.err.println
                    ("You also might have to set global variables FHEMPL (path to fhem.pl) and " +
                            "FHEMPORT (fhem telnet port) in your $HOME/.profile");
            System.err.println("Otherwise, is your telnet port password protected by FHEM? " +
                    "Client Mode won't work if this is the case because of FHEM. Use telnet directly with a password.");
            fnfe.printStackTrace();
        }
    }
}
