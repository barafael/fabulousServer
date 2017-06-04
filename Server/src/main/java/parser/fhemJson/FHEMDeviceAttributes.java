package parser.fhemJson;

import java.util.HashMap;

/**
 * Created by ra on 02.06.17.
 */

/* Don't change attribute names! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:
   @SerializedName("oldname")

   javac warns about unused elements because of gson.
 */
@SuppressWarnings("unused")

class FHEMDeviceAttributes {
    private String coordX;
    private String coordY;
    private String model;
    private String room;
}
