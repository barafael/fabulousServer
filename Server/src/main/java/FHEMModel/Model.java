package FHEMModel;

import FHEMModel.sensors.Room;
import FHEMModel.sensors.Sensor;
import FHEMModel.timeserie.Timeserie;

import java.util.HashSet;

/**
 * @author Rafael
 */

public class Model {
    private final HashSet<Sensor> sensors;
    private final HashSet<Room> rooms;
    private final HashSet<Timeserie> timeseries;

    public Model(HashSet<Sensor> sensors, HashSet<Room> rooms, HashSet<Timeserie> timeseries) {
        this.sensors = sensors;
        this.rooms = rooms;
        this.timeseries = timeseries;
    }
}
