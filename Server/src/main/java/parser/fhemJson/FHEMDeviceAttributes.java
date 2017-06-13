package parser.fhemJson;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

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
    /* Json attributes */
    private String coordX;
    private String coordY;
    private String model;
    /* More than one room is possible! */
    @SerializedName("room")
    private String rooms;
    @SerializedName("name_in_app")
    private String nameInApp;

    private String subType;

    private String permissions;
    private String alias;

    public Optional<String> getSubType() {
        return Optional.ofNullable(subType);
    }

    public Optional<String> getRooms() {
        return Optional.ofNullable(rooms);
    }

    int getCoordX() {
        if (coordX == null) coordX = "0";
        if (coordX.isEmpty()) coordX = "0";
        return Integer.parseInt(coordX);
    }

    int getCoordY() {
        if (coordY == null) coordY = "0";
        if (coordY.isEmpty()) coordY = "0";
        return Integer.parseInt(coordY);
    }

    /* Field because getPermissions() sounds weird */
    Optional<String> getPermissionField() {
        return Optional.ofNullable(permissions);
    }

    public Optional<String> getNameInApp() {
        return Optional.ofNullable(nameInApp);
    }

    public Optional<String> getAlias() {
        return Optional.ofNullable(alias);
    }
}
