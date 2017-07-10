package WebServer.FHEMParser.fhemJson;

import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Rafael
 * @date 02.06.17.
 * This class represents the relevant attributes of the
 * elements of the 'Results' array in jsonList2.json.
 */

@SuppressWarnings("unused")
public class FHEMDevice {
    /* Class Attributes */
    /* Only ever valid for FileLog devices */
    transient private String linkedDeviceName;
    /* Json Attributes */
    @SerializedName("Name")
    private String name;
    @SerializedName("Internals")
    private FHEMDeviceInternals internals;
    @SerializedName("Attributes")
    private FHEMDeviceAttributes attributes;
    @SerializedName("Readings")
    private FHEMDeviceReadings readings;

    /** 
     * Accessor method for the internals of a FHEM device.
     * */
    FHEMDeviceInternals getInternals() {
        /* invariant:
           Internals != null
           This would mean that the underlying FHEM device had no Internals section!
        */
        if (internals == null) {
            System.err.println("Internals was null! We assumed this could never be the case." +
                    "FHEM usually sets this field.");
        }
        return internals;
    }

    /** 
     * This method returns if the device this class represents was tagged as a sensor in FHEM.
     * FHEM itself does not distinguish between devices, which is why this
     * crude measure of tagging manually is necessary.
     */
    boolean isSensor() {
        boolean isSupersensor = getInternals().isSupersensor();
        return !isSupersensor && isInRoom("sensors");
    }

     /** 
     * This method returns if the device this class represents is tagged with a FileLog type in FHEM.
     * FHEM itself does not distinguish between devices, which is why this crude measure of checking manually is necessary.
     * At least in this case, the internals have an attribute called 'TYPE'.
     */
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

    /**
     * Accessor for the name field which is set in jsonList2
     */
    String getName() {
        return name;
    }

    /**
     * This method returns if a device is located in a FHEM room.
     * Manual parsing of a comma-separated line is necessary, because that is how FHEM stores a room.
     * A room exists in FHEM if it is mentioned in the 'room' String in a device's attributes.
     * @param name: Name of the room to check.
     */
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

    /**
     * This method manually checks if a type field was set in jsonList2's internals.
     * @param type: the type to check against
     */
    private boolean isOfType(String type) {
        Optional<String> type_opt = internals.getType();
        if (type_opt.isPresent()) {
            String devType = type_opt.get();
            return devType.equals(type);
        }
        System.err.println(name + ": Encountered a FHEM device without TYPE set!");
        return false;
    }

    /**
     * This method returns if the underlying device should be shown in the app.
     * This is marked by a tag 'app' in the room list.
     */
    private boolean isShowInApp() {
        if (!isSensor() && !isFileLog()) {
            System.err.println("This might be unintended: " +
                    "FHEM device " + name + " is not sensor or log, but is in app room.");
        }
        return this.isInRoom("app");
    }

    /**
     * This method parses a device to a sensor, if it's type fits.
     * All obtainable meta information is also added to the sensor.
     */
    Optional<FHEMSensor> parseToSensor() {
        if (!isSensor()) {
            return Optional.empty();
        }
        int coordX = attributes.getCoordX();
        int coordY = attributes.getCoordY();
        String permissionField = attributes.getPermissionField().orElse("");
        List<String> permissions = Arrays.asList(permissionField.split(","));
        HashMap<String, String> meta = new HashMap<>();

        String alias = attributes.getAlias().orElse("Not supplied");

        FHEMSensor sensor = new FHEMSensor(coordX, coordY, name, alias, permissions, isShowInApp(), meta);

        sensor.setIcon(getAttributes().getIcon());

        /* Add metadata which might or might not be supplied for every sensor */
        sensor.addMeta("State", internals.getState().orElse("Not supplied"));
        sensor.addMeta("Type", internals.getType().orElse("Not supplied"));
        sensor.addMeta("SubType", internals.getType().orElse("Not supplied"));
        readings.getReadings().forEach(sensor::addMeta);

        return Optional.of(sensor);
    }

    /**
     * This method parses this device to a filelog, if it's type fits
     * It also links a timeserie to the new log by it's path.
     */
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
        String permissionField = attributes.getPermissionField().orElse("");
        List<String> permissions = Arrays.asList(permissionField.split(","));

        String path = path_opt.get();
        return Optional.of(new FHEMFileLog(path, name, isShowInApp(), permissions));
    }

    private FHEMDeviceAttributes getAttributes() {
        return attributes;
    }

    /**
     * This method checks if the device is a fakelog.
     */
    boolean isFakelog() {
        return getInternals().getRegexp().orElse("").equals("fakelog");
    }

    /**
     * A filelog is 'approved' if the path to it's timeserie contains the $FHEMDIR/log/timeseries/ directory.
     * It means that the format is such that it can be parsed in a Timeseries easily.
     */
    boolean isApproved() {
        return getInternals().getCurrentLogfileField().orElse("").contains("timeseries");
    }

    /** Two devices are likely to be linked if the filelog's regexp contains the sensor's name.
     * This is stored in linkedDeviceName, and should only be accessed from devices representing FileLogs.
     */
    public boolean isLinked(FHEMSensor s) {
        return linkedDeviceName.equals(s.getName());
    }

    /** 
     * Since rooms have to be used to tag a fhem device, a special prefix must be used for real rooms.
     * Those rooms are where the device should appear in the app, hence the name.
     */
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

    public FHEMDeviceReadings getReadings() {
        return readings;
    }
}

