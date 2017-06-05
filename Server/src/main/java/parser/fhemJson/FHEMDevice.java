package parser.fhemJson;

import java.util.Optional;

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
           This would mean that the underlying FHEM device had no Internals section!
        */
        if (Internals == null) {
            /* Explicit assumption validated! */
            System.err.println("Internals was 0! We assumed this could never be the case.");
        }
        return Internals;
    }

    public boolean isSensor() {
        Optional<String> rooms_opt = Attributes.getRooms();
        if (rooms_opt.isPresent()) {
            String rooms = rooms_opt.get();
            /* allow for some commas and whitespace, but include 'app' */
            /* A .contains() is not enough because some room might include it ("appartment") */
            /* IDEA includes a nifty regex checker (just put cursor in regex). */
            String pattern = "(.*,\\s?)*app(,\\s?.*)*";
            return rooms.matches(pattern);
        }
        return false;
    }

    public boolean isFileLog() {
        Optional<String> type_opt = Internals.getType();
        if (type_opt.isPresent()) {
            String type = type_opt.get();
            return type.equals("FileLog");
        }
        return false;
    }
}
