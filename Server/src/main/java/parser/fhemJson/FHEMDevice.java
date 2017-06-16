package parser.fhemJson;

import fhemModel.sensors.Room;
import fhemModel.sensors.Sensor;
import fhemModel.timeserie.FHEMFileLog;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.*;

/**
 * @author Rafael
 * @date 02.06.17.
 * This class represents the relevant attributes of the
 * elements of the 'Results' section in each device in jsonList2.
 */

/* Don't change attribute names or remove attributes! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   javac warns about unused elements because of gson.
 */

@SuppressWarnings("unused")

public class FHEMDevice {
    /* Json Attributes */
    @SerializedName("Name")
    private String name;
    @SerializedName("Internals")
    private FHEMDeviceInternals internals;
    @SerializedName("Attributes")
    private FHEMDeviceAttributes attributes;

    /* Class Attributes */
    private final HashSet<FHEMDevice> linkedDevices = new HashSet<>();
    /* Used to give filelogs and sensors an ID */
    private static long IDCounter = 0;
    /* Only ever valid for FileLog devices */
    private String linkedDeviceName;

    public String getName() {
        return name;
    }

    public FHEMDeviceInternals getInternals() {
        /* invariant:
           Internals != null
           This would mean that the underlying FHEM device had no Internals section!
        */
        if (internals == null) {
            /* Explicit assumption violated! */
            System.err.println("Internals was null! We assumed this could never be the case." +
                    "FHEM usually sets this field.");
        }
        return internals;
    }

    public boolean isSensor() {
        return isInRoom("sensors");
    }

    public boolean isFileLog() {
        if (isOfType("FileLog")) {
            /* Consistency check: A FileLog must have a currentlogfile attribute too */
            if (!this.getInternals().getCurrentLogfileField().isPresent()) {
                System.err.println("Encountered LogFile without associated currentlogfile field!");
            }
            /* End consistency check */
            return true;
        }
        return false;
    }

    private boolean isInRoom(String name) {
        Optional<String> rooms_opt = attributes.getRooms();
        if (rooms_opt.isPresent()) {
            String rooms = rooms_opt.get();
            /* Allow for some commas and whitespace, but include 'name' */
            /* A .contains() is not enough because some room might include it ("app" and "appartment") */
            /* IDEA includes a nifty regex checker (just put cursor in regex). */
            String pattern = "(.*,\\s?)*" + name + "(,\\s?.*)*";
            return rooms.matches(pattern);
        }
        return false;
    }

    private boolean isOfType(String type) {
        Optional<String> type_opt = internals.getType();
        if (type_opt.isPresent()) {
            String devType = type_opt.get();
            return devType.equals(type);
        }
        System.err.println(name + ": Encountered a FHEM device without TYPE set!");
        return false;
    }

    boolean isShowInApp() {
        if (!isSensor() && !isFileLog()) {
            System.err.println("This might be unintended: " +
                    "FHEM device " + name + " is not sensor or log, but is in app room.");
        }
        return this.isInRoom("app");
    }

    Optional<Sensor> parseToSensor() {
        if (!isSensor()) {
            return Optional.empty();
        }
        int coordX = attributes.getCoordX();
        int coordY = attributes.getCoordY();
        long ID = IDCounter++;
        String permissions = attributes.getPermissionField().orElse("");
        String status = internals.getState().orElse("Not supplied");
        boolean showInApp = isShowInApp();
        HashMap<String, String> meta = new HashMap<>();

        Sensor s = new Sensor(coordX, coordY, name, ID, permissions, isShowInApp(), meta);

        /* Add metadata which might or might not be supplied for every sensor */
        s.addMeta("State", internals.getState().orElse("Not supplied"));
        s.addMeta("Type", internals.getType().orElse("Not supplied"));
        s.addMeta("SubType", internals.getType().orElse("Not supplied"));
        /* TODO use those in android app, don't just dump them */
        s.addMeta("name_in_app", attributes.getNameInApp().orElse("Not supplied"));
        s.addMeta("alias", attributes.getAlias().orElse("Not supplied"));

        return Optional.of(s);
    }

    Optional<FHEMFileLog> parseToLog() {
        if (!isFileLog()) {
            return Optional.empty();
        }
        /*  Extract sensor from regex
         *  FHEM uses this regex internally on a device's filelog to filter events which belong in this filelog.
         *  A regex must be present, starting with the sensor name this data belongs to.
         *  */
        if (!getInternals().getRegexpPrefix().isPresent()) {
            System.err.println("FileLog has no REGEXP prefix: " + getName());
            return Optional.empty();
        }
        Optional<String> path_opt = internals.getCurrentLogfileField();
        if (!path_opt.isPresent()) {
            System.err.println("No logfile specified for log: " + getName());
            return Optional.empty();
        }
        String path = path_opt.get();
        try {
        return Optional.of(new FHEMFileLog(path, isShowInApp()));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    boolean associate(FHEMDevice fhemDevice) {
        return linkedDevices.add(fhemDevice);
    }

    boolean associate(List<FHEMDevice> linkedFilelogs) {
        return linkedDevices.addAll(linkedFilelogs);
    }

    boolean isLinked(FHEMDevice device) {
        return linkedDevices.contains(device);
    }

    public FHEMDeviceAttributes getAttributes() {
        return attributes;
    }

    public Optional<List<Room>> getRooms() {
        Optional<String> rooms_opt = getAttributes().getRooms();
        if (rooms_opt.isPresent()) {
            String rooms_str = rooms_opt.get();
            String[] rooms = rooms_str.split(",");
            List<Room> roomList = new ArrayList<>();
            for (String roomname : rooms) {
                roomList.add(new Room(roomname));
            }
            return Optional.of(roomList);
        } else {
            return Optional.empty();
        }
    }

    boolean isFakelog() {
        Optional<String> regexp = getInternals().getRegexp();
        /* Short circuit logic: if !isPresent(), get() will never be evaluated */
        return regexp.isPresent() && regexp.get().equals("fakelog");
    }

    public boolean isBlessed() {
        Optional<String> path = getInternals().getCurrentLogfileField();
        return path.isPresent() && path.get().contains("timeseries");
    }
}
