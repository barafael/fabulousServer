package parser.fhemJson;

import com.google.gson.annotations.SerializedName;
import fhemModel.Model;
import fhemModel.sensors.Room;
import fhemModel.sensors.Sensor;
import fhemModel.timeserie.Timeserie;

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
    private final String arg = "";
    /**
     * List of all defined FHEM devices matching devicespec (if given; else all of them)
     **/
    private final FHEMDevice[] Results = null;
    /**
     * Number of devices in FHEM matching the <devicespec> (or all of them if empty)
     **/
    private int totalResultsReturned;

    public String getArg() {
        return arg;
    }

    public Model toFHEMModel() {
        HashSet<FHEMDevice> sensors = new HashSet<>();
        HashSet<Room> rooms = new HashSet<>();
        HashSet<FHEMDevice> filelogs = new HashSet<>();

        /* Ignore static analysis: Results is populated by gson */
        for (FHEMDevice d : Results) {
            boolean isSensor = d.isSensor();
            boolean isFileLog = d.isFileLog();
            /* Either it is one of filelog or sensor, or it is neither */
            assert (isSensor ^ isFileLog) | (!isFileLog && !isSensor);

            if (isSensor) {
                sensors.add(d);
            } else if (isFileLog) {
                if (d.getInternals().getCurrentLogfileField().get().contains("timeseries")) {
                    filelogs.add(d);
                }
            }
        }

        for (FHEMDevice filelog : filelogs) {
            Optional<String> sensorname_opt = filelog.getInternals().getRegexpPrefix(':');
            if (!sensorname_opt.isPresent()) {
                System.err.println("Could not detect which sensor corresponds to this filelog: " + filelog.getName());
                break;
            }
            String sensorname = sensorname_opt.get();
            List<FHEMDevice> associated = sensors.stream()
                    .filter(d -> d.getName().equals(sensorname)).collect(Collectors.toList());
            if (associated.size() != 1) {
                System.err.println("Found " + associated.size() + " sensors for FileLog " + filelog.getName());
                break;
            } else {
                /* Associate this filelog with it's sensor */
                filelog.associate(associated.get(0));
            }
        }
        for (FHEMDevice sensor: sensors) {
            List<FHEMDevice> linkedFilelogs = filelogs.stream()
                    .filter(f -> f.isLinked(sensor)).collect(Collectors.toList());
            sensor.associate(linkedFilelogs);
        }

        HashSet<Sensor> realSensors;
        HashSet<Timeserie> realTimeseries;

        realSensors = sensors.stream().map(FHEMDevice::parseToSensor)
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toCollection(HashSet::new));
        realTimeseries = filelogs.stream().map(FHEMDevice::parseToLog)
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toCollection(HashSet::new));

        // Gson gson = new Gson();

        // System.out.println("All sensors: " + gson.toJson(realSensors));

        // System.out.println("All filelogs: " + gson.toJson(realTimeseries));

        // System.out.println(gson.toJson(gson));

        //TODO find a way to do rooms
        return new Model(realSensors, null, realTimeseries);
    }
}
