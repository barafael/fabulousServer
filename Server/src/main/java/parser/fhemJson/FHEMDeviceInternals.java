package parser.fhemJson;

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
    private String DEF;
    private String STATE;
    private String REGEXP;
    private String TYPE;
    private String currentlogfile;

    Optional<String> getRegexpPrefix() {
        return getRegexpPrefix(':');
    }

    Optional<String> getRegexpPrefix(char sep) {
        int index = REGEXP.indexOf(sep);
        if (index >= 0) {
            String prefix = REGEXP.substring(0, index);
            return prefix.equals("") ? Optional.empty() : Optional.of(prefix);
        } else {
            /* 'sep' not present */
            return Optional.empty();
        }
    }

    Optional<String> getCurrentLogfileField() {
        return currentlogfile != null ? Optional.of(currentlogfile) : Optional.empty();
    }
}
