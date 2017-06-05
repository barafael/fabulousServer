package FHEMModel;

import FHEMModel.sensors.Room;
import FHEMModel.sensors.Sensor;
import FHEMModel.timeserie.Timeserie;

import java.util.HashSet;

/**
 * @author Rafael
 */

public class Model {
    private HashSet<Sensor> sensors;
    private HashSet<Room> rooms;
    private HashSet<Timeserie> timeseries;

    public Model(HashSet<Sensor> sensors, HashSet<Room> rooms, HashSet<Timeserie> timeseries) {
        this.sensors = sensors;
        this.rooms = rooms;
        this.timeseries = timeseries;
    }
}
