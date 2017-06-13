package parser.fhemJson;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/**
 * Created by ra on 02.06.17.
 * This class represents the relevant attributes of the
 * 'Internals' section in each device in jsonList2.
 */

/* Don't change attribute names or remove attributes! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   javac warns about unused elements because of gson.
 */

@SuppressWarnings("unused")
public class FHEMDeviceInternals {
    /* Json Attributes */
    @SerializedName("DEF")
    private String definition;
    @SerializedName("STATE")
    private String state;
    @SerializedName("REGEXP")
    private String regexp;
    @SerializedName("TYPE")
    private String type;
    private String currentlogfile;

    Optional<String> getRegexpPrefix() {
        return getRegexpPrefix(':');
    }

    Optional<String> getRegexpPrefix(char sep) {
        int index = regexp.indexOf(sep);
        if (index >= 0) {
            String prefix = regexp.substring(0, index);
            return prefix.isEmpty() ? Optional.empty() : Optional.of(prefix);
        } else {
            /* 'sep' not present */
            return Optional.of(regexp);
        }
    }

    Optional<String> getCurrentLogfileField() {
        return Optional.ofNullable(currentlogfile);
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<String> getState(){
        return Optional.ofNullable(state);
    }
}
