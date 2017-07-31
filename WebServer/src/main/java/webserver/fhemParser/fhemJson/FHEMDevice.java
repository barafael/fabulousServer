package webserver.fhemParser.fhemJson;

import com.google.gson.annotations.SerializedName;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Rafael 02.06.17.
 *         This class represents the relevant attributes of the
 *         elements of the 'Results' array in jsonList2.json.
 */
@SuppressWarnings("unused")
public final class FHEMDevice {
    /* Class Attributes */
    /**
     * This field contains the name of a linked device.
     * It should only ever be valid for FileLog devices.
     */
    private transient String linkedDeviceName;
    /* Json Attributes */
    /**
     * Name as defined in FHEM.
     */
    @SerializedName("Name")
    private String name;

    /**
     * Json Map in jsonList2, contains internal FHEM information and state.
     */
    @SerializedName("Internals")
    private FHEMDeviceInternals internals;

    /**
     * Json Map in jsonList2, contains attributes like coordinates and other user attributes.
     */
    @SerializedName("Attributes")
    private FHEMDeviceAttributes attributes;

    /**
     * Json Map in jsonList2, contains readings which are value-time pairs and may contain useful information.
     */
    @SerializedName("Readings")
    private FHEMDeviceReadings readings;

    /**
     * This method parses a device to a sensor, if it's type fits.
     * All obtainable meta information is also added to the sensor.
     *
     * @return the parsed sensor, if parsing was possible
     */
    Optional<FHEMSensor> parseToSensor() {
        if (!isSensor()) {
            return Optional.empty();
        }
        int coordX = attributes.getCoordX();
        int coordY = attributes.getCoordY();
        List<String> permissions = attributes.getPermissionList();
        HashMap<String, String> meta = new HashMap<>();

        String alias = attributes.getAlias();
        String de_alias = attributes.getDeAlias();
        String en_alias = attributes.getEnAlias();
        String ar_alias = attributes.getArAlias();

        FHEMSensor sensor = new FHEMSensor(
                coordX,
                coordY,
                name,
                alias,
                de_alias,
                en_alias,
                ar_alias,
                permissions,
                isVisibleInApp(),
                meta,
                attributes.getFuseTag());

        sensor.setIcon(attributes.getIcon());

        /* Add metadata which might or might not be supplied for every sensor */
        if (alias.contains("Licht")) {
            String reading = internals.getState().orElse("No brightness reading supplied");
            reading = reading.substring(3) + " Lux";
            sensor.addMeta("Brightness", reading);
        }
        //Optional<String> sub_opt = attributes.getSubType();
        //sub_opt.ifPresent(s ->
        //        sensor.addMeta("Subtype", s));
        Optional<String> reading_opt = internals.getState();
        if (reading_opt.isPresent()) {
            String reading = reading_opt.get();
            Map<String, String> map = readings.getReadings();
            if (!map.values().contains(reading)) {
                if (reading.startsWith("Usage: ")) {
                    sensor.addMeta("Usage", reading.substring(7));
                } else if (reading.startsWith("Temperature: ")) {
                    sensor.addMeta("Temperature", reading.substring(13) + "°C");
                } else if (reading.startsWith("Disk_Usage: ")) {
                    sensor.addMeta("DiskUsage", reading.substring(12));
                } else if (!reading.equals("???") && !reading.matches("([^:]+:){2,}.*")) {
                    sensor.addMeta("Reading", reading);
                }
            }
        }
        //sensor.addMeta("Type", internals.getType().orElse("Not supplied"));
        //sensor.addMeta("SubType", internals.getType().orElse("Not supplied"));
        readings.getReadings().forEach(sensor::addMeta);

        List<String> importantFields = attributes.getImportantFields();
        for (String field : importantFields) {
            if (!sensor.getMeta().containsKey(field)) {
                System.err.println("The field " + field
                        + " was marked as important in FHEM but was not found in the readings of the sensor "
                        + sensor.getName());
            }
        }
        sensor.addImportantFields(importantFields);

        return Optional.of(sensor);
    }

    /**
     * This method parses this device to a filelog, if it's type fits
     * It also links a timeserie to the new log by it's path.
     *
     * @return the parsed filelog, if parsing was possible
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
            System.err.println("FileLog has no REGEXP prefix: " + name);
            return Optional.empty();
        }
        Optional<String> path_opt = internals.getCurrentLogfileField();
        if (!path_opt.isPresent()) {
            System.err.println("No logfile specified for log: " + name);
            return Optional.empty();
        }
        List<String> permissions = attributes.getPermissionList();

        boolean switchable = isInRoom("actuators");

        String path = path_opt.get();
        return Optional.of(new FHEMFileLog(path, name, switchable, permissions));
    }

    /**
     * This method returns if the device this class represents was tagged as a sensor in FHEM.
     * FHEM itself does not distinguish between devices, which is why this
     * crude measure of tagging manually is necessary.
     *
     * @return whether the device is a sensor
     */
    boolean isSensor() {
        boolean isSupersensor = getInternals().isSupersensor();
        return !isSupersensor && isInRoom("sensors");
    }

    /**
     * This method returns if the underlying device should be shown in the app.
     * This is marked by a tag 'app' in the room list.
     *
     * @return whether the room should be shown in the app
     */
    private boolean isVisibleInApp() {
        if (!isSensor() && !isFileLog()) {
            System.err.println("This might be unintended: "
                    + "FHEM device " + name + " is not sensor or log, but is in app room.");
        }
        return this.isInRoom("app");
    }

    /**
     * Getter for the attributes of this device.
     *
     * @return this device's attributes
     */
    private FHEMDeviceAttributes getAttributes() {
        return attributes;
    }

    /**
     * Accessor method for the internals of a FHEM device.
     *
     * @return the device's internals
     */
    FHEMDeviceInternals getInternals() {
        /* invariant:
           Internals != null
           This would mean that the underlying FHEM device had no Internals section!
        */
        if (internals == null) {
            System.err.println("Internals was null! We assumed this could never be the case."
                    + "FHEM usually sets this field.");
        }
        return internals;
    }

    /**
     * This method returns if a device is located in a FHEM room.
     * Manual parsing of a comma-separated line is necessary, because that is how FHEM stores a room.
     * A room exists in FHEM if it is mentioned in the 'room' String in a device's attributes.
     *
     * @param roomName roomName of the room to check.
     * @return if this device lies in the room given by roomName
     */
    private boolean isInRoom(String roomName) {
        Optional<String> rooms_opt = attributes.getRooms();
        if (rooms_opt.isPresent()) {
            String rooms = rooms_opt.get();
            /* Allow for some commas and whitespace, but include 'name' */
            /* A .contains() is not enough because some room might include it ("app" and "appartment") */
            /* IDEA includes a nifty regex checker (just put cursor in regex). */
            String pattern = "(.*,\\s?)*" + roomName + "(,\\s?.*)*";
            return rooms.matches(pattern);
        }
        return false;
    }

    /**
     * This method returns if the device this class represents is tagged with a FileLog type in FHEM.
     * FHEM itself does not distinguish between devices, which is why this
     * crude measure of checking manually is necessary.
     * At least in this case, the internals have an attribute called 'TYPE'.
     *
     * @return whether the device is a filelog
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
     * This method manually checks if a type field was set in jsonList2's internals.
     *
     * @param type the type to check against
     * @return whether the device is of given type
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
     * Accessor for the name field which is set in jsonList2.
     *
     * @return the name from jsonList2
     */
    String getName() {
        return name;
    }

    /**
     * This method checks if the device is a fakelog.
     *
     * @return whether this device is marked as a fakelog
     */
    boolean isFakelog() {
        return getInternals().getRegexp().orElse("").equals("fakelog");
    }

    /**
     * A filelog is 'approved' if the path to it's timeserie contains the $FHEMDIR/log/timeseries/ directory.
     * It means that the format is such that it can be parsed in a Timeseries easily.
     *
     * @return whether the timeseries for this filelog is approved for display
     */
    boolean isApproved() {
        return getInternals().getCurrentLogfileField().orElse("").contains("timeseries");
    }

    /**
     * Two devices are likely to be linked if the filelog's regexp contains the sensor's name.
     * This is stored in linkedDeviceName, and should only be accessed from devices representing FileLogs.
     *
     * @param sensor the other sensor
     * @return whether this and the other sensor is linked
     */
    public boolean isLinked(FHEMSensor sensor) {
        return linkedDeviceName.equals(sensor.getName());
    }

    /**
     * Getter for this device's readings.
     *
     * @return the device's readings
     */
    public FHEMDeviceReadings getReadings() {
        return readings;
    }

    /**
     * Since rooms have to be used to tag a fhem device, a special prefix must be used for real rooms.
     * Those rooms are where the device should appear in the app, hence the name.
     *
     * @return the room in which this device lies, which is annotated with prefix 'room_'
     */
    FHEMRoom getAppRoom() {
        Optional<String> rooms_opt = attributes.getRooms();
        if (!rooms_opt.isPresent()) {
            System.err.println("Found a device which is not in any room. Adding it to orphan room. " + name);
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
            System.err.println("Found a device which belongs to no app room. Adding it to orphan room. " + name);
            appRoom = new FHEMRoom("room_orphaned");
        } else {
            appRoom = appRooms.get(0);
        }
        return appRoom;
    }
}

