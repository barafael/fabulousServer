package fhemModel;

import fhemModel.sensors.Room;
import fhemModel.sensors.FHEMSensor;
import fhemModel.timeserie.FHEMFileLog;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class FHEMModel {
    private final HashSet<FHEMSensor> sensors;
    private final HashSet<Room> rooms;
    private final HashSet<FHEMFileLog> timeseries;

    public FHEMModel(HashSet<Sensor> sensors, HashSet<Room> rooms, HashSet<FHEMFileLog> timeseries) {
        this.sensors = sensors;
        this.rooms = rooms;
        this.timeseries = timeseries;
    }

    public HashSet<Room> getAppRooms() {
        return rooms.stream().filter(Room::isAppRoom).collect(Collectors.toCollection(HashSet::new));
    }

    public HashSet<FHEMSensor> getSensors() {
        return sensors;
    }

    public HashSet<Room> getRooms() {
        return rooms;
    }

    public HashSet<FHEMFileLog> getTimeseries() {
        return timeseries;
    }
}
