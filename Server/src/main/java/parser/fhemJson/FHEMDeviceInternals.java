package parser.fhemJson;

import java.util.Optional;

/**
 * Created by ra on 02.06.17.
 */

/* Don't change attribute names! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

  @SerializedName("oldname")

 */

public class FHEMDeviceInternals {
    private String DEF;
    private String NAME;
    private String STATE;
    private String REGEXP;
    private String currentlogfile;

    Optional<String> getRegexpPrefix() {
        return getRegexpPrefix(':');
    }

    Optional<String> getRegexpPrefix(char sep) {
        int index = REGEXP.indexOf(sep);
        if (index >= 0) {
            REGEXP.subSequence(0, index);
            return REGEXP != "" ? Optional.of(REGEXP) : Optional.empty();
        } else {
            /* 'sep' not present */
            return Optional.empty();
        }
    }

    Optional<String> getCurrentLogfileField() {
        return currentlogfile != null ? Optional.of(currentlogfile) : Optional.empty();
    }
}
