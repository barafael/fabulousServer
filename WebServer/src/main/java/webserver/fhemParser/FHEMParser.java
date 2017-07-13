package webserver.fhemParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import webserver.Main;
import webserver.fhemParser.fhemConnection.FHEMClientModeCon;
import webserver.fhemParser.fhemConnection.FHEMConnection;
import webserver.fhemParser.fhemConnection.FHEMNotFoundException;
import webserver.fhemParser.fhemJson.JsonList2;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.serializers.ModelSerializer;
import webserver.fhemParser.fhemUtils.FHEMUtils;
import webserver.stateCheck.StateChecker;
import webserver.stateCheck.rules.Rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This singleton class provides methods to parse a FHEM model.
 * It owns a FHEM connection, an editing mutex for the server, and a reference to the last parsed model.
 *
 * @author Rafael on 31.05.17.
 */
public final class FHEMParser {
    /**
     * For benchmark time output.
     */
    private static final boolean PRINT_TIME = false;
    /**
     * Timeout for the mutex: 15 minutes.
     */
    private static final long MUTEX_TIMEOUT = 15 * 60 * 1000;
    /**
     * The instance of this singleton.
     */
    private static FHEMParser instance;
    /**
     * The mutex, storing the user name.
     */
    private static String mutex = "";
    /**
     * Flag for mocking; should usually be off.
     */
    private static boolean mock = false;
    /**
     * An instance of {@link webserver.fhemParser.fhemConnection.FHEMConnection FHEMConnection}
     * which this parser will use to obtain information from FHEM.
     */
    private FHEMConnection fhc = new FHEMClientModeCon();

    /**
     * A cache for the currently most recent model.
     */
    private FHEMModel model;

    /**
     * Prevent construction of the Parser. Should only happen via getInstance().
     */
    private FHEMParser() {
    }

    /**
     * Getter for the Parser instance.
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
     * Setter for the {@link webserver.fhemParser.fhemConnection.FHEMConnection FHEM connection}. Returns itself, enabling builder style.
     *
     * @param con the new FHEMConnection.
     * @return the instance of {@link webserver.fhemParser this} with the new parser set.
     */
    public FHEMParser setFHEMConnection(FHEMConnection con) {
        fhc = con;
        return this;
    }

    public Optional<String> getFHEMModelJSON(List<String> permissions) {
        return getFHEMModelJSON(permissions, "rules.json");
    }

    /**
     * This method returns a JSON-serialized view of the current model where all rooms, sensors and filelogs
     * which are not permitted by the passed permissions are filtered out.
     * Returns a string to avoid copying the entire model (as it has to be serialized anyway) and data races on the model.
     * <p>
     * Custom serialization for the FHEM model:
     * First, everything is copied, marking non-permitted fields.
     * Then, the ignored fields are filtered out
     * Most of the work is done in the custom {@link webserver.fhemParser.fhemModel.serializers Gson serializers}.
     *
     * @param permissions a list of permissions which limit what information will be given to the caller.
     * @return a serialized model which, when deserialized, contains only the permitted filelogs, sensors and rooms
     */
    public Optional<String> getFHEMModelJSON(List<String> permissions, String path) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(FHEMModel.class, new ModelSerializer(permissions))
                .create();
        /* Return the mapped Optional.of if present, empty otherwise */
        return getFHEMModel(path).map(gson::toJson);
    }

    public Optional<FHEMModel> getFHEMModel() {
        return getFHEMModel("rules.json");
    }

    /**
     * This method gets a FHEM model. Depending on where it is running, it gets data from different sources,
     * permitting easy mocking of real data. Pulling FHEM data to the local machine: ./pull.sh from the top-lvl dir.
     *
     * @return a FHEMModel, if present
     */
    public Optional<FHEMModel> getFHEMModel(String pathToRules) {
        Instant one = PRINT_TIME ? Instant.now() : null;
        if (System.getProperty("user.home").equals("/home/ra")) {
            mock = true;
        }
        String jsonList2_str = "";
        if (mock) {
            /* Mock jsonlist2! */
            Optional<String> mockdir_opt = FHEMUtils.getGlobVar("FHEMMOCKDIR");
            if (!mockdir_opt.isPresent()) {
                System.err.println("You might have to set the FHEMMOCKDIR variable in your profile!");
                return Optional.empty();
            }
            String path = mockdir_opt.get();
            try {
                jsonList2_str = new String(Files.readAllBytes(Paths.get(path + "jsonList2.json")));
            } catch (IOException e) {
                System.err.println("FHEM might not be running or the logs might not be there.");
                System.err.println(
                        "You also might have to set global variables FHEMDIR (path to dir that contains fhem.pl, like '/opt/fhem/') and\n"
                                + "FHEMPORT (fhem telnet port) in your $HOME/.profile");
                System.err.println("Otherwise, is your telnet port password protected by FHEM? "
                        + "Client Mode won't work if this is the case because of FHEM. You should consider removing the password and instead blocking the port.");
                return Optional.empty();
            }
            if (PRINT_TIME) System.out.println("Got file at: " + Duration.between(one, Instant.now()).toMillis());
            jsonList2_str = jsonList2_str.replaceAll("/opt/fhem/log/", path + "fhemlog/");
            jsonList2_str = jsonList2_str.replaceAll("./log/", path + "fhemlog/");

            if (PRINT_TIME)
                System.out.println("Replaced paths at: " + Duration.between(one, Instant.now()).toMillis());
        } else {
            try {
                jsonList2_str = fhc.getJsonList2();
            } catch (FHEMNotFoundException e) {
                System.err.println("FHEM might not be running or the logs might not be there.");
                System.err.println(
                        "You also might have to set global variables FHEMDIR (path to dir that contains fhem.pl, like '/opt/fhem/') and\n"
                                + "FHEMPORT (fhem telnet port) in your $HOME/.profile");
                System.err.println("Otherwise, is your telnet port password protected by FHEM? "
                        + "Client Mode won't work if this is the case because of FHEM. You should consider removing the password and instead blocking the port.");
                return Optional.empty();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (PRINT_TIME) System.out.println("Got file at: " + Duration.between(one, Instant.now()).toMillis());
        }
        JsonList2 list = JsonList2.parseFrom(jsonList2_str);
        if (PRINT_TIME)
            System.out.println("Parsed jsonlist at: " + Duration.between(one, Instant.now()).toMillis());
        FHEMModel fhemModel = list.toFHEMModel();
        if (PRINT_TIME)
            System.out.println("Made fhem model at: " + Duration.between(one, Instant.now()).toMillis());
        model = fhemModel;
        StateChecker.getInstance().evaluate(model, pathToRules);
        return Optional.ofNullable(fhemModel);
    }

    /**
     * Sets the sensor position of a specific sensor in FHEM.
     * This executes a perl command on the server.
     *
     * @param x          x position in %
     * @param y          y position in %
     * @param sensorName name of sensor
     * @return whether the operation succeeded
     */
    public synchronized boolean setSensorPosition(int x, int y, String sensorName) {
        if ((x > 100 || x < 0) || (y > 100 || y < 0)) {
            x = 50;
            y = 50;
            System.err.printf("Incorrect percent values for coordinates! x: %d, y: %d", x, y);
        }

        if (!model.sensorExists(sensorName)) {
            return false;
        }

        try {
            return fhc.perlCommand("attr " + sensorName + " coordX " + x) &&
                    fhc.perlCommand("attr " + sensorName + " coordY " + y);
        } catch (IOException e) {
            System.err.println("Couldn't talk to FHEM via Client Mode!");
            return false;
        }
    }

    /**
     * Setter for the roomplan of the specified room.
     * It sets the content of the given rooms' file on the disk.
     *
     * @param roomName which should get the new roomplan
     * @param content  the content of the file
     * @return whether the operation succeeded
     */
    public synchronized boolean setRoomplan(String roomName, String content) {
        Optional<FHEMRoom> room_opt = model.getRoomByName(roomName);
        if (room_opt.isPresent()) {
            FHEMRoom room = room_opt.get();
            return room.setRoomplan(content);
        }
        return false;
    }

    /**
     * getter for a roomplan, if sufficient permissions for caller.
     *
     * @param roomName    the desired room's name
     * @param permissions the permissions of the caller
     * @return the roomplan, if caller's permissions suffice.
     */
    public Optional<String> getRoomplan(String roomName, List<String> permissions) {
        if (model == null) {
            getFHEMModel();
        }
        Optional<FHEMRoom> room_opt = model.getRoomByName(roomName);
        if (!room_opt.isPresent()) {
            return Optional.empty();
        }

        FHEMRoom room = room_opt.get();
        if (!room.hasPermittedSensors(permissions)) {
            return Optional.of("null");
        }

        return room.getRoomplan();
    }

    /**
     * getter for a roomplan, if sufficient permissions for caller. Only delivers a plan if the hashes mismatch.
     *
     * @param roomName    the desired room's name
     * @param hash        the hash of the file at the caller
     * @param permissions the permissions of the caller
     * @return the roomplan, if caller's permissions suffice.
     */
    public Optional<String> getRoomplan(String roomName, int hash, List<String> permissions) {
        Optional<FHEMRoom> room_opt = model.getRoomByName(roomName);
        if (!room_opt.isPresent()) {
            return Optional.empty();
        }

        FHEMRoom room = room_opt.get();
        if (!room.hasPermittedSensors(permissions)) {
            return Optional.of("null");
        }
        return room.getRoomplan(hash);
    }

    /**
     * Gets a specific timeserie by fileLog, if the callers permissions suffice.
     *
     * @param fileLogID   ID of filelog (name)
     * @param permissions permissions of caller
     * @return an optional String - the json representation of the timeserie
     */
    public Optional<String> getTimeserie(String fileLogID, List<String> permissions) {
        long now = System.currentTimeMillis() / 1000L;
        return getTimeserie(0, now, fileLogID, permissions);
    }

    /**
     * Gets a specific timeserie by fileLog, if the callers permissions suffice.
     *
     * @param startTime   start time
     * @param endTime     end time
     * @param permissions permissions of caller
     * @param fileLogID   ID of filelog (name)
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

    /**
     * Requests a mutex for a given user.
     *
     * @param username the username
     * @return the ID of a mutex, if successful
     */
    public synchronized Optional<Long> getMutex(String username) {
        if (mutex.isEmpty()) {
            mutex = username;
            long timer = Main.vertx.setTimer(MUTEX_TIMEOUT, event -> releaseMutex(username));
            System.out.println("Parser: Set Mutex for user: " + username);
            return Optional.of(timer);
        } else {
            System.out.println("Parser: Mutex is unavailable");
            return Optional.empty();
        }
    }

    /**
     * Releases the mutex of a given username.
     *
     * @param username the username
     * @return true if the user had the mutex, false otherwise
     */
    public synchronized boolean releaseMutex(String username) {
        if (mutex.equals(username)) {
            mutex = "";
            System.out.println("Parser: released Mutex for user: " + username);
            return true;
        } else {
            System.err.println("Parser: no mutex for user: " + username);
            return false;
        }
    }

    public synchronized boolean setActuator(String sensorName, boolean state, List<String> permissions) {
        //TODO: implement
        return false;
    }

    /**
     * Get the mutex which is set.
     *
     * @return the set mutex
     */
    public String readMutex() {
        return mutex;
    }
}
