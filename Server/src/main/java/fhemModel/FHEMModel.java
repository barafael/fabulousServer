package fhemModel;

import fhemModel.sensors.FHEMRoom;
import fhemModel.sensors.FHEMSensor;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class FHEMModel {
    private final HashSet<FHEMRoom> rooms;

    public FHEMModel(HashSet<FHEMRoom> rooms) {
        this.rooms = rooms;
    }

    public HashSet<FHEMRoom> getAppRooms() {
        return rooms.stream().filter(FHEMRoom::isAppRoom).collect(Collectors.toCollection(HashSet::new));
    }

    public HashSet<FHEMRoom> getRooms() {
        return rooms;
    }
}
