package WebServer.FHEMParser.fhemJson;

import com.google.gson.annotations.SerializedName;
import WebServer.FHEMParser.fhemUtils.FHEMUtils;

import java.util.Optional;

/**
 * Created by ra on 02.06.17.
 * This class represents the relevant attributes of the
 * 'Internals' section in each device in jsonList2.
 */

/* Don't change attribute names or remove attributes! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   Static analysis warns about unused elements because of gson.
   There is a high number of false positives detected in this package due to many fields which are
   only ever initialized by gson. This is intentional and cannot be easily avoided.
 */

@SuppressWarnings("unused")
public class FHEMDeviceInternals {
    /* Json Attributes */
    @SerializedName("DEF")
    private String definition;
    @SerializedName("STATE")
    private String state;
    private String channel_01;
    @SerializedName("REGEXP")
    private String regexp;
    @SerializedName("TYPE")
    private String type;
    private String currentlogfile;

    /**
     * The regexp of a logfile in FHEM usually starts with the name of a sensor.
     * The separator, by default, is a colon.
     * This method can be used to guess a correspondence between a sensor and a log.
     * @return a prefix of the filelog's regexp, if present
     */
    Optional<String> getRegexpPrefix() {
        return getRegexpPrefix(':');
    }

    /**
     * The regexp of a logfile in FHEM always starts with the name of a sensor.
     * This private helper method does the actual work of finding a regexpprefix.
     * This method can be used to guess a correspondence between a sensor and a log.
     * @return a prefix of the filelog's regexp, if present
     */
    private Optional<String> getRegexpPrefix(char sep) {
        int index = regexp.indexOf(sep);
        if (index >= 0) {
            String prefix = regexp.substring(0, index);
            return prefix.isEmpty() ? Optional.empty() : Optional.of(prefix);
        } else {
            /* 'sep' not present */
            return Optional.of(regexp);
        }
    }

    /**
     * Gets an expanded and normalized path to a logfile from the internals of a FileLog.
     * @return an expanded and normalized path to a logfile from the internals of a FileLog
     */
    Optional<String> getCurrentLogfileField() {
        if (currentlogfile.startsWith(".")) {
            Optional<String> fhemPath = FHEMUtils.getGlobVar("FHEMDIR");
            fhemPath.ifPresent(s -> currentlogfile = currentlogfile.replaceFirst(".", s));
            fhemPath.ifPresent(s -> currentlogfile = currentlogfile.replaceAll("//", "/"));
        }
        return Optional.ofNullable(currentlogfile);
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<String> getState(){
        return Optional.ofNullable(state);
    }

    public Optional<String> getRegexp() {
        return Optional.ofNullable(regexp);
    }

    public boolean hasChannels() {
        return channel_01 != null;
    }
}
