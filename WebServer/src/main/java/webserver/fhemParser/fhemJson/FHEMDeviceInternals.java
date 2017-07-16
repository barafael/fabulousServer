package webserver.fhemParser.fhemJson;

import com.google.gson.annotations.SerializedName;
import webserver.fhemParser.fhemUtils.FHEMUtils;

import java.util.Optional;

/**
 * This class represents the relevant attributes of the
 * 'Internals' section in each device in jsonList2.
 *
 * @author Rafael on 02.06.17.
 */
@SuppressWarnings("unused")
public final class FHEMDeviceInternals {
    /* Json Attributes */
    /**
     * This field can be used to filter out the device name a filelog has been defined on.
     */
    @SerializedName("DEF")
    private String definition;
    /**
     * This field holds information about the internals' state in FHEM.
     */
    @SerializedName("STATE")
    private String state;
    /**
     * This fields' presence marks a supersensor which is not useful for the frontend.
     */
    private String channel_01;
    /**
     * This fields' presence marks a supersensor which is not useful for the frontend.
     */
    @SerializedName("Clients")
    private String clients;
    /**
     * This field can be used to filter out the device name a filelog has been defined on.
     */
    @SerializedName("REGEXP")
    private String regexp;
    /**
     * This field holds type information.
     * Usually it is not useful, but in the case of filelogs, the type is set to 'FileLog'.
     */
    @SerializedName("TYPE")
    private String type;
    /**
     * This field holds a path to a fileLog on disk.
     * Date strings like %y-%m are substituted for the current year and month.
     */
    private String currentlogfile;

    /**
     * The regexp of a logfile in FHEM usually starts with the name of a sensor.
     * The separator, by default, is a colon.
     * This method can be used to guess a correspondence between a sensor and a log.
     *
     * @return a prefix of the filelog's regexp, if present
     */
    Optional<String> getRegexpPrefix() {
        return getRegexpPrefix(':');
    }

    /**
     * The regexp of a logfile in FHEM always starts with the name of a sensor.
     * This private helper method does the actual work of finding a regexpprefix.
     * This method can be used to guess a correspondence between a sensor and a log.
     *
     * @param sep the char used as separator
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
     *
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

    public Optional<String> getState() {
        return Optional.ofNullable(state);
    }

    public Optional<String> getRegexp() {
        return Optional.ofNullable(regexp);
    }

    /**
     * A supersensor is not useful for displaying, as it contains technical information about it's children.
     *
     * @return whether the internals belong to a supersensor
     */
    public boolean isSupersensor() {
        return channel_01 != null || clients != null;
    }
}
