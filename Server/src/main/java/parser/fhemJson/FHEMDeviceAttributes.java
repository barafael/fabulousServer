package parser.fhemJson;

/**
 * Created by ra on 02.06.17.
 * This class represents the relevant attributes of the
 * 'Attributes' section in each device in jsonList2.
 */

/* Don't change attribute names or remove attributes! They are needed by Gson to parse jsonList2.
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
    private String name_in_app;
    private String subType;
}
