package webserver.fhemParser.fhemModel.room;

import org.jetbrains.annotations.NotNull;
import webserver.Main;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.fhemParser.fhemUtils.FHEMUtils;
import webserver.ruleCheck.rules.RuleInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
public final class FHEMRoom implements Iterable<FHEMSensor> {
    /**
     * A set of sensors.
     */
    private final Set<FHEMSensor> sensors = new HashSet<>();
    /**
     * The name of this room.
     */
    private final String name;
    /**
     * A path to a hash file which should be saved on disk and contain the hash of the current plan.
     */
    private final transient Path pathToHash;
    /**
     * A path to the current room plan.
     */
    private final transient Path pathToPlan;

    /**
     * Constructor for this room, which verifies that the room really starts with 'room_' (the prefix for real rooms).
     *
     * @param roomname the name of the new room
     */
    public FHEMRoom(String roomname) {
        if (!roomname.startsWith("room_")) {
            System.err.println("Room name does not start with 'room_'!");
        }
        if (roomname.contains("_")) {
            roomname = roomname.substring(5);
        }
        name = roomname.substring(0, 1).toUpperCase() + roomname.substring(1);
        String fhemPath = FHEMUtils.getGlobVar("FHEMDIR").orElse("");
        pathToPlan = Paths.get(fhemPath + "roomplans/" + roomname + ".png");
        pathToHash = Paths.get(fhemPath + "roomplans/" + roomname + ".hash");
    }

    public String getName() {
        return name;
    }

    /**
     * Getter for this room's sensor set.
     *
     * @return a set of sensors contained in this room
     */
    public Set<FHEMSensor> getSensors() {
        return sensors;
    }

    /**
     * Attempt to manually unify multiple sensors tagged with the same fuseTag to one device.
     */
    public void fuseSensors() {
        Map<String, Set<FHEMSensor>> toFuse = new HashMap<>();
        for (FHEMSensor sensor : sensors) {
            if (sensor.getFuseTag().isEmpty()) {
                continue;
            }
            if (!toFuse.containsKey(sensor.getFuseTag())) {
                toFuse.put(sensor.getFuseTag(), new HashSet<>());
            }
            toFuse.get(sensor.getFuseTag()).add(sensor);
        }
        for (Map.Entry<String, Set<FHEMSensor>> entry : toFuse.entrySet()) {
            String newName = entry.getKey();

            FHEMSensor union = entry.getValue().stream().filter(s -> s.getName().endsWith("_union"))
                    .findAny().orElseThrow(() ->
                            new RuntimeException("Union sensor not found! " + newName));

            List<String> permissions = new ArrayList<>();
            for (FHEMSensor sensor : entry.getValue()) {
                permissions.addAll(sensor.getPermissions());
            }
            HashMap<String, String> meta = new HashMap<>();
            for (FHEMSensor sensor : entry.getValue()) {
                meta.putAll(sensor.getMeta());
            }
            FHEMSensor newSensor = new FHEMSensor(union.getCoordX(),
                    union.getCoordY(),
                    union.getName(),
                    newName,
                    union.getDeAlias(),
                    union.getEnAlias(),
                    union.getArAlias(),
                    permissions,
                    true,
                    meta,
                    union.getFuseTag());
            for (FHEMSensor sensor : entry.getValue()) {
                for (FHEMFileLog log : sensor.getLogs()) {
                    newSensor.addLog(log);
                }
                for (RuleInfo info : sensor.getRuleInfos()) {
                    newSensor.addRuleInfo(info);
                }
            }
            newSensor.setIcon(union.getIcon());
            sensors.removeIf(s -> entry.getValue().contains(s));
            sensors.add(newSensor);
        }
    }

    /**
     * Add a sensor to this room.
     *
     * @param sensor the sensor to add
     */
    public void addSensor(@NotNull FHEMSensor sensor) {
        sensors.add(sensor);
    }

    /**
     * This method checks if this room was annotated with the 'app' tag in FHEM.
     *
     * @return whether this room should be shown in the frontend
     */
    public boolean isAppRoom() {
        return name.startsWith("room_");
    }

    /**
     * This method checks if there are sensors with given permissions in this room.
     *
     * @param permissions the callers permissions
     * @return true if there are available sensors
     */
    public boolean hasPermittedSensors(List<String> permissions) {
        for (FHEMSensor sensor : this) {
            if (sensor.hasPermittedLogs(permissions) || sensor.hasPermittedRules(permissions)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure
     * while not permitting mutable access.
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

    /**
     * Gets a plan corresponding to this room.
     *
     * @return a plan if present, empty otherwise.
     */
    public Optional<String> getRoomplan() {
        try {
            String content = new String(Base64.getEncoder().encode(Files.readAllBytes(pathToPlan)));
            return Optional.of(content);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * This method gets a roomplan, depending on the hash given.
     * If the hashes of the caller and the roomplan are equal, there are no changes and the file is not delivered.
     * Else, the file is delivered.
     *
     * @param request_hash the hash of the caller's file
     * @return the roomplan, if there were changes. Empty otherwise.
     */
    public Optional<String> getRoomplan(long request_hash) {
        try {
            String hash_str = new String(Files.readAllBytes(pathToHash));
            long file_hash = Long.parseLong(hash_str);
            if (Main.SERVER_DBG) System.out.println("Hash-check: File: "+file_hash+" vs. Request"+request_hash);
            if (request_hash != file_hash) {
                return Optional.of(new String(Base64.getEncoder().encode(Files.readAllBytes(pathToPlan))));
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Setter for a roomplan, which writes the roomplan to a file.
     *
     * @param picture the file's content
     * @return whether the operation succeeded
     */
    public boolean setRoomplan(String picture) {
        System.out.println("FHEMRoom: "+this.getName() + " setRoomplan");
        try{
            System.out.println("Writing Plan to: "+pathToPlan);
            byte[] decodedImg = Base64.getDecoder().decode(picture.getBytes(StandardCharsets.UTF_8));
            Files.write(pathToPlan, decodedImg);
            System.out.print("Writing Hash to: "+pathToHash);
            BufferedWriter hashwrite = Files.newBufferedWriter(pathToHash);
            hashwrite.write(String.valueOf(picture.hashCode()));
            hashwrite.close();
        } catch (Exception e) {
            System.err.println("Failed to save Roomplan!");
            e.printStackTrace();
            return false;
        }
        System.out.println("finished setRoomplan with success");
        return true;
    }

    @Override
    public int hashCode() {
        int result = pathToHash != null ? pathToHash.hashCode() : 0;
        result = 31 * result + (pathToPlan != null ? pathToPlan.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FHEMRoom that = (FHEMRoom) o;

        if (pathToHash != null ? !pathToHash.equals(that.pathToHash) : that.pathToHash != null) return false;
        if (pathToPlan != null ? !pathToPlan.equals(that.pathToPlan) : that.pathToPlan != null) return false;
        return name.equals(that.name);
    }
}
