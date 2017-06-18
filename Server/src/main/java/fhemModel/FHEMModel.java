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
}
