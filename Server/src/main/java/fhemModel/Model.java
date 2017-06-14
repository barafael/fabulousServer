package fhemModel;

import fhemModel.sensors.Room;
import fhemModel.sensors.Sensor;
import fhemModel.timeserie.Timeserie;

import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public HashSet<Room> getAppRooms() {
        return rooms.stream().filter(Room::isAppRoom).collect(Collectors.toCollection(HashSet::new));
    }

    public HashSet<Sensor> getSensors() {
        return sensors;
    }

    public HashSet<Room> getRooms() {
        return rooms;
    }

    public HashSet<Timeserie> getTimeseries() {
        return timeseries;
    }
}
