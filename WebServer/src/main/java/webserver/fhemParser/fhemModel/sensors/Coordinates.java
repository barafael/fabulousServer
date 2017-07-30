package webserver.fhemParser.fhemModel.sensors;

import com.google.gson.annotations.SerializedName;

/**
 * Class to store coordinates of a sensor in FHEM.
 * Coordinates may be positive integer values.
 *
 * @author Rafael
 */
//TODO before final: privatize this, only public for testing
final class Coordinates {
    /**
     * A coordinate in x between 0 (inclusive) and 100 (inclusive).
     */
    @SerializedName("x")
    private final int coordX;

    /**
     * A coordinate in y between 0 (inclusive) and 100 (inclusive).
     */
    @SerializedName("y")
    private final int coordY;

    /**
     * This constructor ensures the coordinates are in the proper range.
     *
     * @param coordX integer between 0 and 100
     * @param coordY integer between 0 and 100
     */
    Coordinates(int coordX, int coordY) {
        if (coordX >= 0 && coordX <= 100) {
            this.coordX = coordX;
        } else {
            this.coordX = 50;
        }
        if (coordY >= 0 && coordY <= 100) {
            this.coordY = coordY;
        } else {
            this.coordY = 50;
        }
    }

    @Override
    public int hashCode() {
        int result = coordX;
        result = 31 * result + coordY;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return coordX == that.coordX && coordY == that.coordY;
    }

    public int getX() {
        return coordX;
    }

    public int getY() {
        return coordY;
    }
}
