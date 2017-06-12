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
    private String coordX;
    private String coordY;
    private String model;
    /* More than one room is possible! */
    @SerializedName("room")
    private String rooms;
    private String name_in_app;

    public Optional<String> getSubType() {
        return Optional.ofNullable(subType);
    }

    private String subType;
    private String permissions;
    private String alias;

    public Optional<String> getRooms() {
        if (rooms != null) {
            return Optional.of(rooms);
        }
        return Optional.empty();
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

    /* Name because getPermissions() sounds weird */
    String getPermissionField() {
        return permissions;
    }
}
