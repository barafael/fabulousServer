package parser.fhemJson;

/**
 * Created by ra on 02.06.17.
 */

/* Don't change attribute names! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

  @SerializedName("oldname")

 */
class FHEMDeviceInternals {
    private String DEF;
    private String NAME;
    private String STATE;
    private String REGEXP;
    private String currentlogfile;
}
