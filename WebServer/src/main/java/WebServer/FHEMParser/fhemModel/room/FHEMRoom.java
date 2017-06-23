package WebServer.FHEMParser.fhemModel.room;

import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemUtils.FHEMUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * This class represents a room which associates a FHEM model.
 *
 * @author Rafael
 */

public class FHEMRoom implements Iterable<FHEMSensor> {
    Set<FHEMSensor> sensors = new HashSet<>();
    private final SVGRoomPlan plan;
    private final String name;

    public FHEMRoom(String roomname) {
        if (roomname.startsWith("room_")) {
            String pathToPlan = FHEMUtils.getGlobVar("FHEMDIR") + "roomplans/" + roomname + ".svg";
            plan = SVGRoomPlan.loadFile(pathToPlan);
        } else {
            plan = null; // Null Plan.
        }
        name = roomname;
    }

    public Set<FHEMSensor> getSensors() {
        return sensors;
    }

    public void addSensor(@NotNull FHEMSensor sensor) {
        sensors.add(sensor);
    }

    public boolean isAppRoom() {
        return name.startsWith("room_");
    }

    @NotNull
    @Override
    public Iterator<FHEMSensor> iterator() {
        return sensors.iterator();
    }

    @Override
    public String toString() {
        return "FHEMRoom{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FHEMRoom room = (FHEMRoom) o;

        return name.equals(room.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }

    public FHEMRoom getSubSensors(List<String> permissions) {
        FHEMRoom room = new FHEMRoom(this.getName());
        for (FHEMSensor sensor : getSensors()) {
            // if (sensor.hasPermission())
        }
        return null;
    }

    @Override
    public void forEach(Consumer<? super FHEMSensor> action) {
        sensors.forEach(action);
    }

    public boolean hasPermittedSensors(List<String> permissions) {
        for (FHEMSensor sensor : this) {
            if (sensor.hasPermittedLogs(permissions)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPermitted(List<String> permissions) {
        return hasPermittedSensors(permissions);
    }
}
