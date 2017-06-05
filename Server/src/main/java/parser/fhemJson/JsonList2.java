package parser.fhemJson;

import FHEMModel.Model;
import FHEMModel.sensors.Room;

import java.util.HashSet;
import java.util.Optional;

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

@SuppressWarnings("unused")

public class JsonList2 {
    /**
     * Arguments which were passed to jsonList2 FHEM command (grammar: <devicespec> <value1> <value2> ...)
     **/
    private String Arg = "";
    /**
     * List of all defined FHEM devices matching devicespec (if given; else all of them)
     **/
    private FHEMDevice[] Results = null;
    /**
     * Number of devices in FHEM matching the <devicespec> (or all of them if empty)
     **/
    private int totalResultsReturned;

    public String getArg() {
        return Arg;
    }

    public int getTotalResultsReturned() {
        return totalResultsReturned;
    }

    //TODO do not ever pass out a pointer to a mutable internal structure (in production).
    // possible fix: clone
    public Optional<FHEMDevice[]> getResults() {
        return Results == null ? Optional.empty() : Optional.of(Results);
    }

    public Model toFHEMModel() {
        HashSet<FHEMSensor> sensors = new HashSet<>();
        HashSet<Room> rooms = new HashSet<>();
        HashSet<FHEMFileLog> filelogs = new HashSet<>();

        for (FHEMDevice d : Results) {
            boolean isSensor = d.isSensor();
            boolean isFileLog = d.isFileLog();
            /* Either it is one of filelog or sensor, or it is neither */
            assert (isSensor ^ isFileLog) | (!isFileLog && !isSensor);

        }
        //TODO parse sensors and logs, link them, and then map parseToLog/parseToSensor on each entry
        return null;
    }
}
