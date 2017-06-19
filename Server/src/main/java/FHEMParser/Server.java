package FHEMParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

import FHEMParser.fhemConnection.FHEMConnection;
import FHEMParser.fhemConnection.FHEMClientModeCon;
import FHEMParser.fhemConnection.FHEMNotFoundException;
import FHEMParser.fhemModel.FHEMModel;
import FHEMParser.fhemUtils.FHEMUtils;
import FHEMParser.fhemJson.JsonList2;

/**
 * // TODO: You should also set pretty printing off in Gson because otherwise the network might drown in pretty, useless whitespace.
 *
 * @author Rafael on 31.05.17.
 */
class Server {
    public static void main(String[] args) {
        FHEMConnection fhc = new FHEMClientModeCon();
        try {
            String jsonList2_str = fhc.getJsonList2();
            /* Mock jsonlist2! */
            if (!System.getProperty("user.home").contains("pi")) {
                Optional<String> mockdir_opt = FHEMUtils.getGlobVar("FHEMMOCKDIR");
                if (mockdir_opt.isPresent()) {
                    String path = mockdir_opt.get();
                    jsonList2_str = new String(Files.readAllBytes(Paths.get(path + "jsonList2.json")));
                    jsonList2_str = jsonList2_str.replaceAll("/opt/fhem/log/", path + "fhemlog/");
                    jsonList2_str = jsonList2_str.replaceAll("./log/", path + "fhemlog/");
                } else {
                    System.err.println("You might have to set the FHEMMOCKDIR variable in your profile!");
                }
            }
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
