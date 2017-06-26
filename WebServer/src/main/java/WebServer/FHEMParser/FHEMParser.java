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

    /**
     * Acquisitor for Parser instance
     *
     * @return the instance of this singleton
     */
    public static synchronized FHEMParser getInstance() {
        if (FHEMParser.instance == null) {
            FHEMParser.instance = new FHEMParser();
        }
        return FHEMParser.instance;
    }

    /**
     * Custom serialization for the FHEM model:
     * First, everything is copied, ignoring non-permitted fields.
     * Then, the ignored fields are filtered out (this happens in the serializers).
     *
     * @param permissions a list of permissions which limit what information will be given to the caller.
     * @return a serialized model which, when deserialized, contains only the permitted filelogs
     */
    public Optional<String> getFHEMModel(List<String> permissions) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(FHEMFileLog.class, new FilelogSerializer(permissions))
                .registerTypeAdapter(FHEMSensor.class, new SensorSerializer(permissions))
                .registerTypeAdapter(FHEMRoom.class, new RoomSerializer(permissions))
                .registerTypeAdapter(FHEMModel.class, new ModelSerializer(permissions))
                .create();
        return getFHEMModel().map(gson::toJson);
    }

    /**
     * This method gets a fhemmodel. Depending on where it is running, it gets data from different sources,
     * permitting easy mocking of real data. Pulling the data: ./pull.sh from the top-lvl dir.
     *
     * @return a FHEMModel, if present
     */
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

                if (timePrint)
                    System.out.println("Replaced stuff at: " + Duration.between(one, Instant.now()).toMillis());

                JsonList2 list = JsonList2.parseFrom(jsonList2_str);
                if (timePrint)
                    System.out.println("Parsed jsonlist at: " + Duration.between(one, Instant.now()).toMillis());
                FHEMModel fhemModel = list.toFHEMModel();
                if (timePrint)
                    System.out.println("Made fhem model at: " + Duration.between(one, Instant.now()).toMillis());
                model = fhemModel;
                return Optional.ofNullable(fhemModel);
            } else {
                System.err.println("You might have to set the FHEMMOCKDIR variable in your profile!");
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the sensor position of a specific sensor in FHEM
     *
     * @param x          x position in %
     * @param y          y position in %
     * @param sensorName name of sensor
     * @return whether the operation succeeded
     */
    public boolean setSensorPosition(int x, int y, String sensorName) {
        FHEMClientModeCon con = new FHEMClientModeCon();
        try {
            return con.sendPerlCommand("attr " + sensorName + " coordX " + x) &&
                    con.sendPerlCommand("attr " + sensorName + " coordY " + y);
        } catch (IOException e) {
            System.err.println("Couldn't talk to FHEM via Client Mode!");
            return false;
        }
    }

    public boolean setRoomplan(String roomName, String svg) {
        Optional<FHEMRoom> room_opt = model.getRoomByName(roomName);
        if (room_opt.isPresent()) {
            FHEMRoom room = room_opt.get();
            room.setPlan(svg);
            return true;
        }
        return false;
    }

    public Optional<String> getRoomplan(String roomName, List<String> permission) {
        return Optional.empty();
    }

    public Optional<String> getRoomplan(String roomName, long hash, List<String> permission) {
        Optional<FHEMRoom> room_opt = model.getRoomByName(roomName);
        if (room_opt.isPresent()) {
            FHEMRoom room = room_opt.get();
            boolean permitted = room.hasPermittedSensors(permission);
            if (!permitted) {
                return Optional.empty();
            }
            return room.getRoomplan(hash);
        }
        return Optional.empty();
    }

    /**
     * Gets a specific timeserie
     *
     * @param fileLogID ID of filelog (name)
     * @return an optional String - the json representation of the timeserie
     */
    public Optional<String> getTimeserie(String fileLogID, List<String> permissions) {
        long now = System.currentTimeMillis() / 1000L;
        return getTimeserie(0, now, fileLogID, permissions);
    }

    /**
     * Gets a specific timeserie
     *
     * @param startTime start time
     * @param endTime   end time
     * @param fileLogID ID of filelog (name)
     * @return an optional String - the json representation of the timeserie
     */
    public Optional<String> getTimeserie(long startTime, long endTime, String fileLogID, List<String> permissions) {
        for (Iterator<FHEMFileLog> it = model.eachLog(); it.hasNext(); ) {
            FHEMFileLog log = it.next();
            if (log.getName().equals(fileLogID)) {
                if (log.isPermitted(permissions)) {
                    return log.subSection(startTime, endTime);
                }
            }
        }
        return Optional.empty();
    }
}
