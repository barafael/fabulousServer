package webserver.fhemParser.fhemModel.sensors;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.stateCheck.rules.RuleInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This class represents the data about a sensor gathered from FHEM.
 * It also contains the FileLog stubs needed to acquire timeseries.
 *
 * @author Rafael
 */
public final class FHEMSensor implements Iterable<FHEMFileLog> {
    private final Coordinates coords;
    private final String name;
    @SerializedName ("alias")
    private final String nameInApp;
    @SuppressWarnings ("FieldCanBeLocal")
    private final transient List<String> permissions;
    private final HashSet<FHEMFileLog> fileLogs = new HashSet<>();
    @SuppressWarnings ("FieldCanBeLocal")
    private final transient boolean isShowInApp;
    private final HashMap<String, String> metaInfo;
    private String icon;
    private Set<RuleInfo> ruleInfos = new HashSet<>();

    public FHEMSensor(int coordX, int coordY, String name, String nameInApp, List<String> permissions,
                      boolean isShowInApp, HashMap<String, String> metaInfo) {
        this.coords = new Coordinates(coordX, coordY);
        this.name = name;
        this.nameInApp = nameInApp;
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

    public Set<RuleInfo> getRuleInfo() {
        return ruleInfos;
    }

    public Optional<FHEMFileLog> getLogByName(String filelogName) {
        for (FHEMFileLog log : fileLogs) {
            if (log.getName().equals(filelogName)) {
                return Optional.of(log);
            }
        }
        return Optional.empty();
    }

    public void addRuleInfo(RuleInfo ruleInfo) {
        /* First remove the old info, then put the current one */
        /* This works because the equals() and hashCode() methods only care about the name */
        if (ruleInfos.contains(ruleInfo)) {
            ruleInfos.remove(ruleInfo);
            ruleInfos.add(ruleInfo);
        } else {
            ruleInfos.add(ruleInfo);
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, FHEMSensor.class);
    }

    public HashSet<FHEMFileLog> getLogs() {
        return fileLogs;
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     * while not permitting mutable access.
     *
     * @return an iterator over the contained logs in this sensor.
     */
    @NotNull
    @Override
    public Iterator<FHEMFileLog> iterator() {
        return fileLogs.iterator();
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     */
    @Override
    public void forEach(Consumer<? super FHEMFileLog> action) {
        fileLogs.forEach(action);
    }

    /**
     * Returns whether any of the logs are permitted to be accessed with the given permissions.
     *
     * @param permissions list of permissions against which to check
     *
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

    public Optional<String> getValueOfField(String field) {
        switch (field) {
            //TODO implement more fields!
            case "STATE":
                return Optional.ofNullable(metaInfo.get("STATE"));
            case "State":
                return Optional.ofNullable(metaInfo.get("State"));
            default:
                return Optional.empty();
        }
    }
}
