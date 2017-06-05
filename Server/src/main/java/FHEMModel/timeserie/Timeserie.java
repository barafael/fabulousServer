package FHEMModel.timeserie;

import java.util.ArrayList;
import java.util.List;

import FHEMModel.sensors.Sensor;
import com.sun.istack.internal.NotNull;

/**
 * This class represents a chronological, sequential list of samples obtained from a FileLog in FHEM.
 * @author Rafael
 */

public class Timeserie {
    private List<Sample> samples;
    private Sensor sensor;
    private String unit;
    private boolean isShowInApp;

    public Timeserie(String path) {
        readFromFile(path);
    }

    public List<Sample> readFromFile(String path) {
        // TODO method stub
        // also set unit from here
        // also set sensor name from here
        return new ArrayList<>();
    }

    public void setSensor(@NotNull Sensor s) {
        sensor = s;
    }

    void setUnit(@NotNull String unit) {
        this.unit = unit;
    }
}
