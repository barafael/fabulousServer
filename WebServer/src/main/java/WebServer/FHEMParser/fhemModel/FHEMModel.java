package WebServer.FHEMParser.fhemModel;

import WebServer.FHEMParser.fhemModel.sensors.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemModel.timeserie.FHEMFileLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class FHEMModel implements Iterable<FHEMSensor> {
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

    public FHEMModel subModel(List<String> permissions) {

        return null;
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

    @NotNull
    @Override
    public Iterator<FHEMSensor> iterator() {
        HashSet<FHEMSensor> sensors = new HashSet<>();
        for (FHEMRoom room : rooms) {
            sensors.addAll(room.getSensors());
        }
        return sensors.iterator();
    }

    @Override
    public void forEach(Consumer<? super FHEMSensor> action) {
        HashSet<FHEMSensor> sensors = new HashSet<>();
        for (FHEMRoom room : rooms) {
            sensors.addAll(room.getSensors());
        }
        sensors.forEach(action);
    }
}
