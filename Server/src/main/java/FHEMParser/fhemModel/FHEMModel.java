package FHEMParser.fhemModel;

import FHEMParser.fhemModel.sensors.FHEMRoom;

import java.util.HashSet;

/**
 * @author Rafael
 */

public class FHEMModel {
    private final HashSet<FHEMRoom> rooms;

    public FHEMModel(HashSet<FHEMRoom> rooms) {
        this.rooms = rooms;
    }
}
