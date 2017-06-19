package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import fhemConnection.FHEMConnection;
import fhemConnection.FHEMClientModeCon;
import com.google.gson.GsonBuilder;
import fhemConnection.FHEMNotFoundException;
import com.google.gson.Gson;
import fhemModel.FHEMModel;
import parser.fhemJson.JsonList2;

/**
 * // TODO: when deployed, remove mocking code and debug output. You should also set pretty printing of in
 * // TODO Gson because otherwise the network might drown in pretty, useless whitespace.
 * @author Rafael on 31.05.17.
 */
class Server {

    public static void main(String[] args) {
        FHEMConnection fhc = new FHEMClientModeCon();
        try {
            @SuppressWarnings("UnusedAssignment") String jsonList2_str = fhc.getJsonList2();

            /* Mock jsonlist2! */
            jsonList2_str = new String(Files.readAllBytes(Paths.get(System.getProperty("user.home") + "/Uni/4.Semester/MESP/fabulousServer/Server/jsonList2.json")));
            jsonList2_str = jsonList2_str.replaceAll("/opt/fhem/log/", System.getProperty("user.home") + "/fhemlog/");
            jsonList2_str = jsonList2_str.replaceAll("./log/", System.getProperty("user.home") + "/fhemlog/");

            JsonList2 list = JsonList2.parseFrom(jsonList2_str);

            FHEMModel fhemModel = list.toFHEMModel();
            /* The other way around - just for testing.
            *  (Convert objects to json again and print)
            * */
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(fhemModel));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FHEMNotFoundException fnfe) {
            System.err.println("FHEM might not be running.");
            System.err.println
                    ("You also might have to set global variables FHEMDIR (path to dir that contains fhem.pl, like '/opt/fhem/') and\n" +
                            "FHEMPORT (fhem telnet port) in your $HOME/.profile");
            System.err.println("Otherwise, is your telnet port password protected by FHEM? " +
                    "Client Mode won't work if this is the case because of FHEM. Use telnet directly with a password.");
            fnfe.printStackTrace();
        }
    }
}
