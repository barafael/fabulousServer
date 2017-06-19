package WebServer.FHEMParser.fhemModel;

import WebServer.FHEMParser.fhemModel.sensors.FHEMRoom;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashSet;

/**
 * @author Rafael
 */

public class FHEMModel {
    private final HashSet<FHEMRoom> rooms;

    public FHEMModel(HashSet<FHEMRoom> rooms) {
        this.rooms = rooms;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
