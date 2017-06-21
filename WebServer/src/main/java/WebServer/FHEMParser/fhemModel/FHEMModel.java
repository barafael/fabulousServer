package WebServer.FHEMParser.fhemModel;

import WebServer.FHEMParser.fhemModel.sensors.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemModel.timeserie.FHEMFileLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class FHEMModel {
    private final HashSet<FHEMRoom> rooms;

    public FHEMModel(HashSet<FHEMRoom> rooms) {
        this.rooms = rooms;
    }

    public Optional getTimeserie(String filelogName) {
        for (FHEMRoom room : rooms) {
            for (FHEMSensor sensor : room.getSensors()) {
                Optional<FHEMFileLog> log = sensor.getLogByName(filelogName);
                if (log.isPresent()) {
                    return log.get().getTimeserie();
                }
            }
        }
        return Optional.empty();
    }

    public Optional<FHEMRoom> getRoom(String roomname) {
        return rooms.stream().filter(r -> r.getName().equals(roomname)).findFirst();
    }

    public String subModel(List<String> permissions) {
        HashSet<FHEMRoom> subrooms = rooms.stream()
                .map(room -> room.getSubSensors(permissions))
                .collect(Collectors.toCollection(HashSet::new));
        return "";
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
        //JsonObject json = new JsonObject().put("test","test2");
        //return json.encode();
    }
}
