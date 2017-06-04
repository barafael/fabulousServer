package parser.fhemJson;

/**
 * Created by ra on 02.06.17.
 * This class represents the relevant attributes of the
 * elements of the 'Results' section in each device in jsonList2.
 */

/* Don't change attribute names or remove attributes! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   javac warns about unused elements because of gson.
 */

@SuppressWarnings("unused")

public class FHEMDevice {
    private String Name;
    private FHEMDeviceInternals Internals;
    private FHEMDeviceAttributes Attributes;

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
