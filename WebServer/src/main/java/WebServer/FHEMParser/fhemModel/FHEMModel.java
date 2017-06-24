package WebServer.FHEMParser.fhemModel;

import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Rafael
 */

public class FHEMModel implements Iterable<FHEMRoom> {
    private final HashSet<FHEMRoom> rooms;

    public FHEMModel(HashSet<FHEMRoom> rooms) {
        this.rooms = rooms;
    }

    private Optional getTimeserie(String filelogName) {
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

    private Optional<FHEMRoom> getRoom(String roomname) {
        return rooms.stream().filter(r -> r.getName().equals(roomname)).findFirst();
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /** Iterator over the contained rooms.
     * This is necessary to encapsulate the rooms collection -
     * Otherwise it would be necessary to give out mutable access to the internal structure.
     * @return an iterator over the rooms in this model
     */
    @NotNull
    @Override
    public Iterator<FHEMRoom> iterator() {
        return rooms.iterator();
    }

    public Iterator<FHEMFileLog> eachLog() {
        HashSet<FHEMFileLog> logs = new HashSet<>();
        forEach(room -> room.forEach(sensor -> logs.addAll(sensor.getLogs())));
        return logs.iterator();
    }

    @Override
    public void forEach(Consumer<? super FHEMRoom> action) {
        rooms.forEach(action);
    }

    public boolean hasPermittedRooms(List<String> permissions) {
        for (FHEMRoom room : this) {
            if (room.isPermitted(permissions)) {
                return true;
            }
        }
        return false;
    }

}
