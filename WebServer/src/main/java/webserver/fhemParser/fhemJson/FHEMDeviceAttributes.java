package webserver.fhemParser.fhemJson;

import com.google.gson.annotations.SerializedName;

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
     * SubType as defined in FHEM. TODO read this in metainfo
     */
    private String subType;
    /**
     * The permissions defined in FHEM.
     */
    private String permissions;
    /**
     * The alias defined in the custom FHEM userattr.
     */
    private String alias;

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
     * Accessor for the X coordinates set in FHEM.
     *
     * @return the x coordinates set
     */
    int getCoordX() {
        if (coordX == null) coordX = "0";
        if (coordX.isEmpty()) coordX = "0";
        return Integer.parseInt(coordX);
    }

    /**
     * Accessor for the Y coordinates set in FHEM.
     *
     * @return the y coordinates set
     */
    int getCoordY() {
        if (coordY == null) coordY = "0";
        if (coordY.isEmpty()) coordY = "0";
        return Integer.parseInt(coordY);
    }

    /**
     * Accessor for the userattr 'permissions' in FHEM.
     * ('Field' because getPermission sounds like a permission inquiry.)
     * @return the userattr 'permissions' in FHEM
     */
    Optional<String> getPermissionField() {
        //TODO handle comma-separation?
        return Optional.ofNullable(permissions);
    }

    /**
     * Acessor for the userattr 'alias' in FHEM.
     *
     * @return the userattr 'alias' in FHEM
     */
    Optional<String> getAlias() {
        return Optional.ofNullable(alias);
    }

    /**
     * Acessor for the userattr 'icon' in FHEM.
     *
     * @return the userattr 'icon_in_app' in FHEM
     */
    String getIcon() {
        return iconInApp;
    }
}
