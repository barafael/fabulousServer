package WebServer.FHEMParser.fhemModel;

import WebServer.FHEMParser.fhemModel.room.FHEMRoom;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    public Optional<FHEMRoom> getRoomByName(String roomname) {
        return rooms.stream().filter(r -> r.getName().equals(roomname)).findFirst();
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this, FHEMModel.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, FHEMModel.class);
    }

    /**
     * This method is necessary to be able to iterate over an internal datastructure while not permitting mutable access.
     *
     * @return an iterator over the contained rooms in this model.
     */
    @NotNull
    @Override
    public Iterator<FHEMRoom> iterator() {
        return rooms.iterator();
    }

    /**
     * This method is necessary to be able to iterate over an internal datastructure while not permitting mutable access.
     *
     * @return an iterator over the contained sensors in this model.
     */
    public Iterator<FHEMSensor> eachSensor() {
        HashSet<FHEMSensor> sensors = new HashSet<>();
        forEach((FHEMRoom room) -> room.forEach(sensors::add));
        return sensors.iterator();
    }

    public Iterator<FHEMFileLog> eachLog() {
        HashSet<FHEMFileLog> logs = new HashSet<>();
        forEach(room -> room.forEach(sensor -> logs.addAll(sensor.getLogs())));
        return logs.iterator();
    }

    /**
     * This method is necessary to be able to iterate over an internal datastructure
     */

    @Override
    public void forEach(Consumer<? super FHEMRoom> action) {
        rooms.forEach(action);
    }

    /**
     * Returns whether any of the rooms are permitted to be accesseed with the given permissions.
     *
     * @param permissions list of permissions against which to check
     * @return whether this model contains viewable rooms
     */

    public boolean hasPermittedRooms(List<String> permissions) {
        for (FHEMRoom room : this) {
            if (room.isPermitted(permissions)) {
                return true;
            }
        }
        return false;
    }

    public Optional<FHEMSensor> getSensorByName(String name) {
        for (Iterator<FHEMSensor> it = this.eachSensor(); it.hasNext(); ) {
            FHEMSensor s = it.next();
            if (s.getName().equals(name)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public boolean sensorExists(String sensorName) {
        for (FHEMRoom room : this) {
            for (FHEMSensor sensor : room) {
                if (sensor.getName().equals(sensorName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<FHEMSensor> getSensorsByCollection(Collection<String> sensors) {
        return sensors.stream()
                .map(this::getSensorByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
