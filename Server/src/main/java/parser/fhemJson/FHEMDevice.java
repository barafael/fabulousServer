package parser.fhemJson;

import FHEMModel.sensors.Sensor;
import FHEMModel.timeserie.Timeserie;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

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
    private String Name;
    private FHEMDeviceInternals Internals;
    private FHEMDeviceAttributes Attributes;

    /* Class Attributes */
    private HashSet<FHEMDevice> linkedDevices;
    /* Used to give filelogs and sensors an ID */
    private static long IDCounter = 0;

    public String getName() {
        return Name;
    }

    public FHEMDeviceInternals getInternals() {
        /* ASSERTION of invariant:
           Internals != null
           This would mean that the underlying FHEM device had no Internals section!
        */
        if (Internals == null) {
            /* Explicit assumption validated! */
            System.err.println("Internals was null! We assumed this could never be the case." +
                    "FHEM usually sets this field.");
        }
        return Internals;
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

    private boolean isInRoom(String roomName) {
        Optional<String> rooms_opt = Attributes.getRooms();
        if (rooms_opt.isPresent()) {
            String rooms = rooms_opt.get();
            /* Allow for some commas and whitespace, but include 'roomName' */
            /* A .contains() is not enough because some room might include it ("app" and "appartment") */
            /* IDEA includes a nifty regex checker (just put cursor in regex). */
            String pattern = "(.*,\\s?)*" + roomName + "(,\\s?.*)*";
            return rooms.matches(pattern);
        }
        return false;
    }

    private boolean isOfType(String type) {
        Optional<String> type_opt = Internals.getType();
        if (type_opt.isPresent()) {
            String devType = type_opt.get();
            return devType.equals(type);
        }
        System.err.println(Name + ": Encountered a FHEM device without TYPE set!");
        return false;
    }

    boolean isShowInApp() {
        if (!isSensor() && !isFileLog()) {
            System.err.println("This might be unintended: " +
                    "FHEM device " + Name + " is not sensor or log, but is in app room.");
        }
        return this.isInRoom("app");
    }

    Optional<Sensor> parseToSensor() {
        if (!isSensor()) {
            return Optional.empty();
        }
        int coordX = Attributes.getCoordX();
        int coordY = Attributes.getCoordY();
        long ID = IDCounter++;
        String permissions = Attributes.getPermissionField();
        String status = ""; // TODO: extract this somehow from FHEM (see metaInfo)
        boolean showInApp = isShowInApp();
        HashMap<String, String> meta = new HashMap<>(); // TODO: get this from FHEM via some magic

        return Optional.of(
                new Sensor(coordX, coordY, Name, ID, permissions, status, showInApp, meta)
        );
    }

    Optional<Timeserie> parseToLog() {
        if (!isFileLog()) {
            return Optional.empty();
        }
        Optional<String> path_opt = Internals.getCurrentLogfileField();
        if (path_opt.isPresent()) {
            String path = path_opt.get();
            path = "log/HM_521A72_brightness-2017-06.log"; // TODO remove this mock later (when running on pi)
            return Optional.of(
                    new Timeserie(path)
            );
        }
        return Optional.empty();
    }
}
