package WebServer.FHEMParser.fhemModel.sensors;

import WebServer.FHEMParser.fhemModel.log.FHEMFileLog;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * This class represents the data about a sensor gathered from FHEM.
 * It also contains the FileLog stubs needed to aquire timeseries.
 *
 * @author Rafael
 */

public class FHEMSensor implements Iterable<FHEMFileLog> {
    private final Coordinates coords;
    private final String name;
    transient private final List<String> permissions;
    private final HashSet<FHEMFileLog> fileLogs = new HashSet<>();
    transient private final boolean isShowInApp;
    private final HashMap<String, String> metaInfo;
    private String icon;

    public FHEMSensor(int coordX, int coordY, String name, List<String> permissions,
                      boolean isShowInApp, HashMap<String, String> metaInfo) {
        this.coords = new Coordinates(coordX, coordY);
        this.name = name;
        this.permissions = permissions;
        this.isShowInApp = isShowInApp;
        this.metaInfo = metaInfo;
    }

    public void addMeta(@NotNull String key, @NotNull String value) {
        metaInfo.put(key, value);
    }

    public void addLog(FHEMFileLog log) {
        fileLogs.add(log);
    }

    public String getName() {
        return name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public Optional<FHEMFileLog> getLogByName(String filelogName) {
        for (FHEMFileLog log : fileLogs) {
            if (log.getName().equals(filelogName)) {
                return Optional.of(log);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, FHEMSensor.class);
    }

    public HashSet<FHEMFileLog> getLogs() {
        return fileLogs;
    }

    /**
     * This method is necessary to be able to iterate over an internal datastructure while not permitting mutable access.
     * @return an iterator over the contained logs in this sensor.
     */
    @NotNull
    @Override
    public Iterator<FHEMFileLog> iterator() {
        return fileLogs.iterator();
    }

    /**
     * This method is necessary to be able to iterate over an internal datastructure
     */
    @Override
    public void forEach(Consumer<? super FHEMFileLog> action) {
        fileLogs.forEach(action);
    }

    /**
     * Returns whether any of the logs are permitted to be accesseed with the given permissions.
     * @param permissions list of permissions against which to check
     * @return whether this sensor contains viewable timeseries
     */
    public boolean hasPermittedLogs(List<String> permissions) {
        for (FHEMFileLog log : this) {
            if (log.isPermitted(permissions)) {
                return true;
            }
        }
        return false;
    }

    public Coordinates getCoords() {
        return coords;
    }

    public boolean stateContains(String state) {
        String value = metaInfo.get("State");
        return value != null && value.contains(state);
    }
}
