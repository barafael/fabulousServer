package webserver.fhemParser.fhemJson;

import com.google.gson.annotations.SerializedName;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/* Don't change attribute names or remove attributes! They are needed by Gson to parse jsonList2.
   If you want to rename an attribute, annotate them with:

   @SerializedName("oldname")

   Static analysis warns about unused elements because of gson.
   There is a high number of false positives detected in this package due to many fields which are
   only ever initialized by gson. This is intentional and cannot be easily avoided.
 */

/**
 * This class represents the relevant attributes of the
 * 'Attributes' section in each device in jsonList2.
 *
 * @author Rafael on 02.06.17.
 */
@SuppressWarnings("unused")
class FHEMDeviceAttributes {
    /* Json attributes */
    /**
     * User attribute for coordinates in FHEM. Should range from 0 to 100 inclusive.
     */
    private String coordX;

    /**
     * User attribute for coordinates in FHEM. Should range from 0 to 100 inclusive.
     */
    private String coordY;

    /**
     * The device's model, as specified in the attributes.
     */
    private String model;

    /**
     * The comma-separated string containing room names of the device is
     * a list of rooms this device is annotated with.
     * Rooms in FHEM really are just String tags.
     */
    @SerializedName("room")
    private String rooms;

    /**
     * A user attribute set to the icon name.
     */
    @SerializedName("icon_in_app")
    private String iconInApp;
    /**
     * SubType as defined in FHEM.
     */
    private String subType;
    /**
     * The permissions defined in FHEM.
     */
    private String permissions;
    /**
     * The alias defined in the custom FHEM userattr.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String alias = "NoAlias";

    /**
     * The german alias defined in the custom FHEM userattr.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String de_alias = "NoDeAlias";
    /**
     * The english alias defined in the custom FHEM userattr.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String en_alias = "NoEnAlias";
    /**
     * The arabic alias defined in the custom FHEM userattr.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String ar_alias = "NoArAlias";

    /**
     * Important fields should be shown in the frontend main screen.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String importantFields = "";

    /**
     * A tag used to manually combine multiple fhem sensors into just one device.
     */
    @SerializedName("fuse_as")
    private String fuseTag;

    /**
     * The SubType attribute is set to the sensor type sometimes.
     *
     * @return the value set for SubType in a FHEM device
     */
    public Optional<String> getSubType() {
        return Optional.ofNullable(subType);
    }

    /**
     * This is the accessor method for the rooms, which are an attribute set manually in FHEM.
     *
     * @return the Rooms list set for a device in FHEM
     */
    Optional<String> getRooms() {
        return Optional.ofNullable(rooms);
    }

    /**
     * Getter for the X coordinates set in FHEM.
     *
     * @return the x coordinates set
     */
    int getCoordX() {
        if (coordX == null) coordX = "0";
        if (coordX.isEmpty()) coordX = "0";
        return Integer.parseInt(coordX);
    }

    /**
     * Getter for the Y coordinates set in FHEM.
     *
     * @return the y coordinates set
     */
    int getCoordY() {
        if (coordY == null) coordY = "0";
        if (coordY.isEmpty()) coordY = "0";
        return Integer.parseInt(coordY);
    }

    /**
     * Getter for the userattr 'permissions' in FHEM.
     * Gets a list of permissions for this device, separated by commas.
     *
     * @return the userattr 'permissions' in FHEM, every comma separated entry as new entry in the list
     */
    List<String> getPermissionList() {
        return Arrays.asList(
                (permissions == null ? "" : permissions).split("\\s*,\\s*"));
    }

    /**
     * Get the fusetag.
     * @return the fhem userattr 'fuse_as' of this device
     */
    public String getFuseTag() {
        return fuseTag == null ? "" : fuseTag;
    }

    /**
     * Getter for the userattr 'alias' in FHEM.
     *
     * @return the userattr 'alias' in FHEM
     */
    String getAlias() {
        return alias;
    }

    /**
     * Return the german alias from FHEM.
     * @return the alias of the device in german, set in FHEM
     */
    public String getDeAlias() {
        return de_alias;
    }

    /**
     * Return the english alias from FHEM.
     * @return the alias of the device in english, set in FHEM
     */
    public String getEnAlias() {
        return en_alias;
    }

    /**
     * Return the arabic alias from FHEM.
     * @return the alias of the device in arabic, set in FHEM
     */
    public String getArAlias() {
        return ar_alias;
    }

    /**
     * Getter for the userattr 'icon' in FHEM.
     *
     * @return the userattr 'icon_in_app' in FHEM
     */
    String getIcon() {
        return iconInApp == null ? "" : iconInApp;
    }

    /**
     * Get the important fields as set in FHEM.
     * @return the keys of entries in the {@link FHEMSensor#metaInfo metaInfo Map} which are set to be important in FHEM
     */
    public List<String> getImportantFields() {
        if (importantFields == null || importantFields.isEmpty()) {
            return new ArrayList<>();
        }
        String[] fieldNames = importantFields.split("\\s*,\\s*");
        return new ArrayList<>(Arrays.asList(fieldNames));
    }
}
