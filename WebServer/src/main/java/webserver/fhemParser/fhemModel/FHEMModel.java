package webserver.fhemParser.fhemModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.room.FHEMRoom;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.History;
import webserver.ruleCheck.RuleSnapshot;
import webserver.eventList.EventList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This model represents the information extracted from FHEM.
 *
 * @author Rafael
 */
public final class FHEMModel implements Iterable<FHEMRoom> {
    /**
     * A set of rooms.
     */
    private final HashSet<FHEMRoom> rooms;

    /**
     * A history of occurred rule events.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private History history;

    private EventList calendarEvents;
    /**
     * A collection of snapshots of violated rules.
     */
    private List<RuleSnapshot> snapshots;

    /**
     * Construct a new model with given rooms.
     *
     * @param rooms a set of rooms
     */
    public FHEMModel(HashSet<FHEMRoom> rooms) {
        this.rooms = rooms;
    }

    /**
     * Getter for a specific timeserie.
     *
     * @param filelogName the name of the desired log
     * @return the desired timeserie, if present. Optional.empty otherwise
     */
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

    /**
     * Getter for the specified room.
     *
     * @param roomname the name of the desired room
     * @return the desired room, if present
     */
    public Optional<FHEMRoom> getRoomByName(String roomname) {
        return rooms.stream().filter(r -> r.getName().equals(roomname)).findFirst();
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this, FHEMModel.class);
    }

    /**
     * Convert this model to json using Gson.
     *
     * @return a json representation of this model
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, FHEMModel.class);
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     * while not permitting mutable access.
     *
     * @return an iterator over the contained rooms in this model.
     */
    @NotNull
    @Override
    public Iterator<FHEMRoom> iterator() {
        return rooms.iterator();
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     * The consumer is applied to each room.
     */
    @Override
    public void forEach(Consumer<? super FHEMRoom> action) {
        rooms.forEach(action);
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     * while not permitting mutable access.
     *
     * @return an iterator over the contained logs in this model.
     */
    public Iterator<FHEMFileLog> eachLog() {
        HashSet<FHEMFileLog> logs = new HashSet<>();
        forEach(room -> room.forEach(sensor -> logs.addAll(sensor.getLogs())));
        return logs.iterator();
    }

    public void setEventList(EventList list) {
        if (list == null) {
            return;
        }
        this.calendarEvents = list;
    }

    /**
     * Returns whether any of the rooms are permitted to be accessed with the given permissions.
     *
     * @param permissions list of permissions against which to check
     * @return whether this model contains viewable rooms
     */

    public boolean hasPermittedRooms(List<String> permissions) {
        for (FHEMRoom room : this) {
            if (room.hasPermittedSensors(permissions)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Getter for a specific room, by name.
     *
     * @param name the name of the desired room
     * @return the specified room, if present
     */
    public Optional<FHEMSensor> getSensorByName(String name) {
        for (Iterator<FHEMSensor> it = this.eachSensor(); it.hasNext(); ) {
            FHEMSensor s = it.next();
            if (s.getName().equals(name)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     * while not permitting mutable access.
     *
     * @return an iterator over the contained sensors in this model.
     */
    public Iterator<FHEMSensor> eachSensor() {
        HashSet<FHEMSensor> sensors = new HashSet<>();
        forEach((FHEMRoom room) -> room.forEach(sensors::add));
        return sensors.iterator();
    }

    /**
     * Get a collection of sensors by specifying a collection of sensor names.
     *
     * @param sensors the input collection
     * @return a collection of sensors with the given names in this model
     */
    public Set<FHEMSensor> getSensorsByCollection(Collection<String> sensors) {
        return sensors.stream()
                .map(this::getSensorByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Check for a given sensor.
     *
     * @param sensorName name of the sensor to check
     * @return whether the sensor with this name exists
     */
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

    /**
     * Set the history of this model.
     *
     * @param history the current sequence of events
     */
    public void setHistory(History history) {
        //System.out.println("Current history:");
        //System.out.println(history);
        this.history = history;
    }

    /**
     * Add a state snapshot, which is a rule that is violated at this very moment.
     * Used for push notifications in the frontend.
     *
     * @param newSnapshots a list of current {@link webserver.ruleCheck.RuleSnapshot RuleSnapshots}
     */
    public void addStateSnapshot(List<RuleSnapshot> newSnapshots) {
        this.snapshots = newSnapshots;
    }

    /**
     * Get the n snapshots with highest priority, sorted.
     * This method should only be useful in the frontend.
     *
     * @param howMany the number of snapshots to return
     * @return the n most important snapshots in a list, where the lowest index has the highest priority
     */
    public List<RuleSnapshot> getSnapshots(int howMany) {
        if (howMany < 1) {
            howMany = 1;
        }
        if (howMany > snapshots.size()) {
            howMany = snapshots.size();
        }
        return snapshots.subList(0, howMany - 1);
    }

    /**
     * Get all snapshots, sorted by priority.
     * This method should only be useful in the frontend.
     *
     * @return a list of sorted snapshots
     */
    public List<RuleSnapshot> getSnapshots() {
        return snapshots;
    }
}
