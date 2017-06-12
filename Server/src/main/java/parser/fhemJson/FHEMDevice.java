package parser.fhemJson;

import fhemModel.sensors.Sensor;
import fhemModel.timeserie.Timeserie;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.FileReader;
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
    private FHEMDeviceInternals Internals;
    private FHEMDeviceAttributes Attributes;

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
        int coordX = Attributes.getCoordX();
        int coordY = Attributes.getCoordY();
        long ID = IDCounter++;
        String permissions = Attributes.getPermissionField();
        String status = ""; // TODO: extract this somehow from FHEM (see metaInfo)
        boolean showInApp = isShowInApp();
        HashMap<String, String> meta = new HashMap<>(); // TODO: get this from FHEM via some magic

        Sensor s = new Sensor(coordX, coordY, name, ID, permissions, status, showInApp, meta);
        s.addMeta("State", Internals.getSTATE().orElse("Not supplied"));
        s.addMeta("Type", Internals.getType().orElse("Not supplied"));
        s.addMeta("SubType", Internals.getType().orElse("Not supplied"));
        return Optional.of(s);
    }

    Optional<Timeserie> parseToLog() {
        if (!isFileLog()) {
            return Optional.empty();
        }
        if (!getInternals().getRegexpPrefix().isPresent()) {
            System.err.println("FileLog has no REGEXP prefix: " + getName());
        }
        Optional<String> path_opt = Internals.getCurrentLogfileField();
        if (path_opt.isPresent()) {
            String path = path_opt.get();
            List<String> lines;
            try  {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
                lines = new ArrayList<>();

                String line;
                while((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
                bufferedReader.close();

                /*Optional<String> firstline_opt = flog.findFirst();
                if (!firstline_opt.isPresent()) {
                    System.err.println("Encountered empty FileLog: " + path);
                }
                String sensorname = firstline_opt.get().split(" ")[1];
                linkedDeviceNamelinkedDeviceName = sensorname;
                */
                return Optional.of(
                        new Timeserie(lines, isShowInApp()));
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    boolean associate(FHEMDevice fhemDevice) {
        return linkedDevices.add(fhemDevice);
    }

    boolean isLinked(FHEMDevice device) {
        return linkedDevices.contains(device);
    }

    boolean associate(Collection<FHEMDevice> linkedFilelogs) {
        return linkedDevices.addAll(linkedFilelogs);
    }

    public FHEMDeviceAttributes getAttributes() {
        return Attributes;
    }
}
