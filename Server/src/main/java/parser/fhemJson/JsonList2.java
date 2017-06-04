package parser.fhemJson;

/**
 * @author Rafael
 */

/* Don't change attribute names! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   javac warns about unused elements because of gson.
 */
@SuppressWarnings("unused")

public class JsonList2 {
    private String Arg;
    private FHEMDevice[] Results;
    private int totalResultsReturned;

    public String getArg() {
        return Arg;
    }

    public int getTotalResultsReturned() {
        return totalResultsReturned;
    }

    public FHEMDevice[] getResults() {
        return Results;
    }

}
