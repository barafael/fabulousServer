package webserver.fhemParser.fhemJson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * This class represents the relevant attributes of jsonList2.
 * All the members in this class have attributes which correspond to relevant fields.
 * The names are used by gson to bind them, which means they should not be removed or their name changed
 * (unless you know what you are doing, i.e. adding annotations instead or
 * really removing attributes from the representation.)
 */

/* If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   Static analysis warns about unused elements because of gson.
   There is a high number of false positives detected in this package due to many fields which are
   only ever initialized by gson. This is intentional and cannot be easily avoided.
 */

/**
 * This class contains the structure of the FHEM jsonList2.
 * It is needed for deserialization and contains methods to further process the data.
 *
 * @author Rafael
 */
public class JsonList2 {
    /* Json Attributes */
    /**
     * List of all defined FHEM devices matching devicespec (if given; else all of them).
     **/
    @SerializedName("Results")
    private final FHEMDevice[] results = null;
    /**
     * Arguments which were passed to jsonList2 FHEM command
     * (grammar: &lt;devicespec&gt; &lt;value1&gt; &lt;value2&gt; ...).
     */
    @SerializedName("Arg")
    private String arg;
    /**
     * Number of devices in FHEM matching the &lt;devicespec&gt; (or all of them if empty).
     **/
    private int totalResultsReturned;

    /**
     * Parse a jsonList2 object from json using Gson. Fields not present in the json file are left untouched.
     *
     * @param jsonString the input string
     * @return an instance of jsonList2
     */
    public static @NotNull JsonList2 parseFrom(String jsonString) {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();

        return gson.fromJson(jsonString, JsonList2.class);
    }

    /**
     * This method converts a parsed jsonList2 to a FHEMModel.
     *
     * @return a FHEMModel generated from the contents of jsonList2.
     */
    public FHEMModel toFHEMModel() {
        HashSet<FHEMSensor> realSensors = new HashSet<>();
        HashSet<FHEMRoom> rooms = new HashSet<>();
        HashSet<FHEMDevice> filelogs = new HashSet<>();

        /* Ignore static analysis 'null dereference' warnings here: results is populated by Gson */
        //noinspection ConstantConditions
        for (FHEMDevice d : results) {
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
                if (!d.isApproved()) {
                    continue;
                }
                filelogs.add(d);
            }
        }

        for (FHEMDevice filelog : filelogs) {
            Optional<String> sensorname_opt = filelog.getInternals().getRegexpPrefix();
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
