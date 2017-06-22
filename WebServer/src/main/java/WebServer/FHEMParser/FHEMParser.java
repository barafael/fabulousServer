package WebServer.FHEMParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import WebServer.FHEMParser.fhemConnection.FHEMConnection;
import WebServer.FHEMParser.fhemConnection.FHEMClientModeCon;
import WebServer.FHEMParser.fhemConnection.FHEMNotFoundException;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemUtils.FHEMUtils;
import WebServer.FHEMParser.fhemJson.JsonList2;

/**
 * TODO: You should also set pretty printing off in Gson because otherwise the network might drown in pretty, useless whitespace.
 *
 * @author Rafael on 31.05.17.
 */

/* TODO whats up with HM_52CC96_Pwr values? */
public class FHEMParser {
    private static FHEMParser instance;

    /* Prevent construction */
    private FHEMParser() {
    }

    public static synchronized FHEMParser getInstance() {
        if (FHEMParser.instance == null) {
            FHEMParser.instance = new FHEMParser();
        }
        return FHEMParser.instance;
    }

    public Optional<String> getFHEMModel(List<String> permissions) {
        // TODO implement gson exclusionstrategies
        return getFHEMModel().map(FHEMModel::toJson);
    }

    public Optional<FHEMModel> getFHEMModel() {
        Instant one = Instant.now();
        if (System.getProperty("user.home").contains("/pi")) {
            FHEMConnection fhc = new FHEMClientModeCon();
            String jsonList2_str = "";
            try {
                jsonList2_str = fhc.getJsonList2();
            } catch (FHEMNotFoundException e) {
                System.err.println("FHEM might not be running or the logs might not be there.");
                System.err.println
                        ("You also might have to set global variables FHEMDIR (path to dir that contains fhem.pl, like '/opt/fhem/') and\n" +
                                "FHEMPORT (fhem telnet port) in your $HOME/.profile");
                System.err.println("Otherwise, is your telnet port password protected by FHEM? " +
                        "Client Mode won't work if this is the case because of FHEM. Use telnet directly with a password.");
                //e.printStackTrace();
                return Optional.empty();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Got file at: " + Duration.between(one, Instant.now()).toMillis());
            JsonList2 list = JsonList2.parseFrom(jsonList2_str);
            System.out.println("Parsed jsonlist at: " + Duration.between(one, Instant.now()).toMillis());
            FHEMModel fhemModel = list.toFHEMModel();
            System.out.println("Made fhem model at: " + Duration.between(one, Instant.now()).toMillis());
            return Optional.ofNullable(fhemModel);
        } else {
            /* Mock jsonlist2! */
            String jsonList2_str = "";
            Optional<String> mockdir_opt = FHEMUtils.getGlobVar("FHEMMOCKDIR");
            if (mockdir_opt.isPresent()) {
                String path = mockdir_opt.get();
                try {
                    jsonList2_str = new String(Files.readAllBytes(Paths.get(path + "jsonList2.json")));
                } catch (IOException e) {
                    System.err.println("FHEM might not be running or the logs might not be there.");
                    System.err.println
                            ("You also might have to set global variables FHEMDIR (path to dir that contains fhem.pl, like '/opt/fhem/') and\n" +
                                    "FHEMPORT (fhem telnet port) in your $HOME/.profile");
                    System.err.println("Otherwise, is your telnet port password protected by FHEM? " +
                            "Client Mode won't work if this is the case because of FHEM. Use telnet directly with a password.");
                    // e.printStackTrace();
                    return Optional.empty();
                };
                System.out.println("Got file at: " + Duration.between(one, Instant.now()).toMillis());
                jsonList2_str = jsonList2_str.replaceAll("/opt/fhem/log/", path + "fhemlog/");
                jsonList2_str = jsonList2_str.replaceAll("./log/", path + "fhemlog/");

                System.out.println("Replaced stuff at: " + Duration.between(one, Instant.now()).toMillis());

                JsonList2 list = JsonList2.parseFrom(jsonList2_str);
                System.out.println("Parsed jsonlist at: " + Duration.between(one, Instant.now()).toMillis());
                FHEMModel fhemModel = list.toFHEMModel();
                System.out.println("Made fhem model at: " + Duration.between(one, Instant.now()).toMillis());
                return Optional.ofNullable(fhemModel);
            } else {
                System.err.println("You might have to set the FHEMMOCKDIR variable in your profile!");
            }
        }
        return Optional.empty();
    }

    public boolean setSensorPosition(int x, int y, String sensorName) {
        return false;
    }

    public boolean setRoomplan(String roomName, String svg) {
        return false;
    }

    public Optional<String> getRoomplan(String roomName) {
        return Optional.empty();
    }

    public Optional<String> getRoomplan(String roomName, long hash) {
        return Optional.empty();
    }

    public Optional<String> getTimeserie(String fileLogID) {
        return Optional.empty();
    }

    public Optional<String> getTimeserie(long startTime, long endTime, String fileLogID) {
        return Optional.empty();
    }
}
