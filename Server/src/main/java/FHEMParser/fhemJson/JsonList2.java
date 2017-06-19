package FHEMParser.fhemJson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import FHEMParser.fhemModel.FHEMModel;
import FHEMParser.fhemModel.sensors.FHEMRoom;
import FHEMParser.fhemModel.sensors.FHEMSensor;
import FHEMParser.fhemModel.timeserie.FHEMFileLog;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Rafael
 *         This class represents the relevant attributes of jsonList2.
 *         All the members in this class have attributes which correspond to relevant fields.
 *         The names are used by gson to bind them, which means they should not be removed or their name changed
 *         (unless you know what you are doing, i.e. adding annotations instead or
 *         really removing attributes from the representation.)
 */


/* If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   javac warns about unused elements because of gson.
 */

public class JsonList2 {
    /* Json Attributes */
    /**
     * Arguments which were passed to jsonList2 FHEM command (grammar: <devicespec> <value1> <value2> ...)
     **/
    @SerializedName("Arg")
    private String arg;
    /**
     * List of all defined FHEM devices matching devicespec (if given; else all of them)
     **/
    private final FHEMDevice[] Results = null;
    /**
     * Number of devices in FHEM matching the <devicespec> (or all of them if empty)
     **/
    private int totalResultsReturned;

    public static @NotNull JsonList2 parseFrom(String jsonString) {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();

        return gson.fromJson(jsonString, JsonList2.class);
    }

    public FHEMModel toFHEMModel() {
        HashSet<FHEMSensor> realSensors = new HashSet<>();
        HashSet<FHEMRoom> rooms = new HashSet<>();
        HashSet<FHEMDevice> filelogs = new HashSet<>();


        /* Ignore static analysis warnings here: Results is populated by gson */
        for (FHEMDevice d : Results) {
            boolean isSensor = d.isSensor();
            boolean isFileLog = d.isFileLog();
            /* Add raw devices, parse them into sensors and filelogs later */
            if (isSensor) {
                Optional<FHEMSensor> parsedSensor_opt = d.parseToSensor();
                if (!parsedSensor_opt.isPresent()) {
                    System.err.println("Could not parse sensor! " + d.getName());
                    continue;
                }
                FHEMSensor sensor = parsedSensor_opt.get();
                FHEMRoom appRoom = d.getAppRoom();
                if (rooms.contains(appRoom)) {
                    for (FHEMRoom room : rooms) {
                        if (room.equals(appRoom)) {
                            room.addSensor(sensor);
                        }
                    }
                } else {
                    appRoom.addSensor(sensor);
                    rooms.add(appRoom);
                }
                realSensors.add(sensor);
            } else if (isFileLog) {
                if (d.isFakelog()) {
                    continue;
                }
                if (!d.isBlessed()) {
                    continue;
                }
                filelogs.add(d);
            } else {
                continue;
            }
        }

        for (FHEMDevice filelog : filelogs) {
            Optional<String> sensorname_opt = filelog.getInternals().getRegexpPrefix(':');
            if (!sensorname_opt.isPresent()) {
                System.err.println("Could not detect which sensor corresponds to this filelog: " + filelog.getName());
                continue;
            }
            String sensorname = sensorname_opt.get();
            List<FHEMSensor> associated = realSensors.stream()
                    .filter(s -> s.getName().equals(sensorname)).collect(Collectors.toList());
            if (associated.size() != 1) {
                System.err.println("Found " + associated.size() + " sensors for FileLog " + filelog.getName());
            } else {
                FHEMSensor sensor = associated.get(0);
                Optional<FHEMFileLog> log = filelog.parseToLog();
                log.ifPresent(sensor::addLog);
            }
        }
        return new FHEMModel(rooms);
    }
}
