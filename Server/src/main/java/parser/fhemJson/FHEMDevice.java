package parser.fhemJson;

import java.util.HashMap;

/**
 * Created by ra on 02.06.17.
 */

/* Don't change attribute names! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

  @SerializedName("oldname")

 */

public class FHEMDevice {
    private String Name;
    private String PossibleSets;
    private String PossibleAttrs;
    private FHEMDeviceInternals Internals;
    private FHEMDeviceReadings Readings;

    public FHEMDeviceInternals getInternals() {
        /* ASSERTION of invariant:
           Internals != null
        */
        if (Internals == null) {
            /* Explicit assumption validated! */
            System.err.println("Internals was 0! We assumed this could never be the case.");
        }
        return Internals;
    }
}
