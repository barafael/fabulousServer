package WebServer.FHEMParser.fhemModel.room;

import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.FHEMParser.fhemUtils.FHEMUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

/**
 * This class represents a room which consists of several sensors which in turn contain filelogs.
 *
 * @author Rafael
 */

public class FHEMRoom implements Iterable<FHEMSensor> {
    private final Set<FHEMSensor> sensors = new HashSet<>();
    private Path pathToHash;
    private Path pathToPlan;
    private final String name;

    public FHEMRoom(String roomname) {
        name = roomname;
        if (roomname.startsWith("room_")) {
            String fhemPath = FHEMUtils.getGlobVar("FHEMDIR").orElse("");
            pathToPlan = Paths.get(fhemPath + "roomplans/" + roomname + ".svg");
            System.out.println("Path to plan: " + pathToPlan);
            pathToHash = Paths.get(fhemPath + "roomplans/" + roomname + ".hash");
        }
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

    public String getName() {
        return name;
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

    /**
     * This method is necessary to be able to iterate over an internal datastructure while not permitting mutable access.
     *
     * @return an iterator over the contained sensors in this room.
     */

    @NotNull
    @Override
    public Iterator<FHEMSensor> iterator() {
        return sensors.iterator();
    }

    @Override
    public void forEach(Consumer<? super FHEMSensor> action) {
        sensors.forEach(action);
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

    public Optional<String> getRoomplan() {
        try {
            String content = new String(Files.readAllBytes(pathToPlan));
            return Optional.of(content);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<String> getRoomplan(int request_hash) {
        try {
            String hash_str = new String(Files.readAllBytes(pathToHash));
            int file_hash = Integer.parseInt(hash_str);
            if (request_hash != file_hash) {
                return Optional.of(new String(Files.readAllBytes(pathToPlan)));
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public boolean setRoomplan(String svg) {
        try (BufferedWriter writer = Files.newBufferedWriter(pathToPlan)) {
            writer.write(svg);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
