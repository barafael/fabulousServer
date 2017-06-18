package fhemModel.sensors;

import com.google.gson.annotations.SerializedName;

/**
 * Class to store coordinates of a sensor in FHEM.
 * Coordinates may be positive integer values.
 * @author Rafael
 */

public class Coordinates {
    @SerializedName("x")
    private int coordX;

    @SerializedName("y")
    private int coordY;

    Coordinates(int coordX, int coordY) {
        if (coordX >= 0 && coordY >= 0) {
            this.coordX = coordX;
            this.coordY = coordY;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return coordX == that.coordX && coordY == that.coordY;
    }

    @Override
    public int hashCode() {
        int result = coordX;
        result = 31 * result + coordY;
        return result;
    }
}
