package WebServer.FHEMParser.fhemJson;

import WebServer.FHEMParser.fhemModel.sensors.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.annotations.SerializedName;

import java.util.*;
import java.util.stream.Collectors;

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
    /* Class Attributes */
    /* Used to give filelogs and sensors an ID */
    transient private static long IDCounter = 0;
    /* Json Attributes */
    @SerializedName("Name")
    private String name;
    @SerializedName("Internals")
    private FHEMDeviceInternals internals;
    @SerializedName("Attributes")
    private FHEMDeviceAttributes attributes;
    /* Only ever valid for FileLog devices */
    transient private String linkedDeviceName;

    String getName() {
        return name;
    }

    FHEMDeviceInternals getInternals() {
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

    boolean isSensor() {
        return isInRoom("sensors");
    }

    boolean isFileLog() {
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

    private boolean isShowInApp() {
        if (!isSensor() && !isFileLog()) {
            System.err.println("This might be unintended: " +
                    "FHEM device " + name + " is not sensor or log, but is in app room.");
        }
        return this.isInRoom("app");
    }

    Optional<FHEMSensor> parseToSensor() {
        if (!isSensor()) {
            return Optional.empty();
        }
        int coordX = attributes.getCoordX();
        int coordY = attributes.getCoordY();
        long ID = IDCounter++;
        String permissionfield = attributes.getPermissionField().orElse("");
        List<String> permissions = Arrays.asList(permissionfield.split(","));
        HashMap<String, String> meta = new HashMap<>();

        FHEMSensor sensor = new FHEMSensor(coordX, coordY, name, ID, permissions, isShowInApp(), meta);

        sensor.setIcon(getAttributes().getIcon());

        /* Add metadata which might or might not be supplied for every sensor */
        sensor.addMeta("State", internals.getState().orElse("Not supplied"));
        sensor.addMeta("Type", internals.getType().orElse("Not supplied"));
        sensor.addMeta("SubType", internals.getType().orElse("Not supplied"));
        /* TODO use those in android app, don't just dump them */
        sensor.addMeta("alias", attributes.getAlias().orElse("Not supplied"));

        return Optional.of(sensor);
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
        return Optional.of(new FHEMFileLog(path, name, isShowInApp()));
    }

    private FHEMDeviceAttributes getAttributes() {
        return attributes;
    }

    boolean isFakelog() {
        return getInternals().getRegexp().orElse("").equals("fakelog");
    }

    boolean isBlessed() {
        return getInternals().getCurrentLogfileField().orElse("").contains("timeseries");
    }

    public boolean isLinked(FHEMSensor s) {
        return linkedDeviceName.equals(s.getName());
    }

    FHEMRoom getAppRoom() {
        Optional<String> rooms_opt = getAttributes().getRooms();
        if (!rooms_opt.isPresent()) {
            System.err.println("Found a device which is not in any room. Adding it to orphan room. " + getName());
            return new FHEMRoom("room_orphaned");
        }
        String rooms_str = rooms_opt.get();
        String[] rooms = rooms_str.split(",");
        /* Only app rooms start with room_ */
        List<FHEMRoom> appRooms =
                Arrays.stream(rooms).filter(roomname -> roomname.startsWith("room_"))
                        .map(FHEMRoom::new).collect(Collectors.toList());
        FHEMRoom appRoom;
        if (appRooms.size() > 1) {
            System.err.println("Found device which belongs to multiple rooms in the app. Choosing the first one.");
            appRoom = appRooms.get(0);
        } else if (appRooms.size() == 0) {
            System.err.println("Found a device which belongs to no app room. Adding it to orphan room. " + getName());
            appRoom = new FHEMRoom("room_orphaned");
        } else {
            appRoom = appRooms.get(0);
        }
        return appRoom;
    }
}
