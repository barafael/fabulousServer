package WebServer.FHEMParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import WebServer.FHEMParser.fhemConnection.FHEMConnection;
import WebServer.FHEMParser.fhemConnection.FHEMClientModeCon;
import WebServer.FHEMParser.fhemConnection.FHEMNotFoundException;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemUtils.FHEMUtils;
import WebServer.FHEMParser.fhemJson.JsonList2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Rafael on 31.05.17.
 */

public class FHEMParser {
    private static boolean timePrint = false;
    private static FHEMParser instance;

    private FHEMModel model;

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
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions))
                .registerTypeAdapter(FHEMRoom.class, new RoomSerializer(permissions))
                .registerTypeAdapter(FHEMModel.class, new ModelSerializer(permissions))
                .create();
        return getFHEMModel().map(gson::toJson);
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
                return Optional.empty();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (timePrint) System.out.println("Got file at: " + Duration.between(one, Instant.now()).toMillis());
            JsonList2 list = JsonList2.parseFrom(jsonList2_str);
            if (timePrint) System.out.println("Parsed jsonlist at: " + Duration.between(one, Instant.now()).toMillis());
            FHEMModel fhemModel = list.toFHEMModel();
            if (timePrint) System.out.println("Made fhem model at: " + Duration.between(one, Instant.now()).toMillis());
            model = fhemModel;
            return Optional.ofNullable(fhemModel);
        } else {
            /* Mock jsonlist2! */
            String jsonList2_str;
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
                    return Optional.empty();
                }
                if (timePrint) System.out.println("Got file at: " + Duration.between(one, Instant.now()).toMillis());
                jsonList2_str = jsonList2_str.replaceAll("/opt/fhem/log/", path + "fhemlog/");
                jsonList2_str = jsonList2_str.replaceAll("./log/", path + "fhemlog/");

                if (timePrint) System.out.println("Replaced stuff at: " + Duration.between(one, Instant.now()).toMillis());

                JsonList2 list = JsonList2.parseFrom(jsonList2_str);
                if (timePrint) System.out.println("Parsed jsonlist at: " + Duration.between(one, Instant.now()).toMillis());
                FHEMModel fhemModel = list.toFHEMModel();
                if (timePrint) System.out.println("Made fhem model at: " + Duration.between(one, Instant.now()).toMillis());
                model = fhemModel;
                return Optional.ofNullable(fhemModel);
            } else {
                System.err.println("You might have to set the FHEMMOCKDIR variable in your profile!");
            }
        }
        return Optional.empty();
    }

    public boolean setSensorPosition(int x, int y, String sensorName) {
        FHEMClientModeCon con = new FHEMClientModeCon();
        try {
            return  con.sendPerlCommand("attr " + sensorName + " coordX " + x) &&
                    con.sendPerlCommand("attr " + sensorName + " coordY " + y);
        } catch (IOException e) {
            System.err.println("Couldn't talk to FHEM via Client Mode!");
            return false;
        }
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
        long now = System.currentTimeMillis() / 1000L;
        return getTimeserie(0, now, fileLogID);
    }

    public Optional<String> getTimeserie(long startTime, long endTime, String fileLogID) {
        for (Iterator<FHEMFileLog> it = model.eachLog(); it.hasNext(); ) {
            FHEMFileLog log = it.next();
            if (log.getName().equals(fileLogID)) {
                return log.subSection(startTime, endTime);
            }
        }
        return Optional.empty();
    }
}
