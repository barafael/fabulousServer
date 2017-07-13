package webserver.fhemParser.fhemModel.sensors;

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
    /**
     * Name of the sensor in FHEM.
     */
    private final String name;
    /**
     * Name of the sensor as it should be shown in the frontend.
     */
    @SerializedName ("alias")
    private final String nameInApp;
    /**
     * Coordinates of sensor in percent.
     */
    private final Coordinates coords;
    /**
     * Permissions of this sensor as set in FHEM.
     * Unused because of technical reasons:
     * In FHEM, a userattr is always global. And any device will have it.
     * This means that the sensor has a permission field which could be used in the future but is unused now.
     */
    @SuppressWarnings ("FieldCanBeLocal")
    private final transient List<String> permissions;
    /**
     * All filelogs which belong to this sensor.
     */
    private final HashSet<FHEMFileLog> fileLogs = new HashSet<>();
    /**
     * Documents whether this should be shown in the frontend.
     */
    @SuppressWarnings ("FieldCanBeLocal")
    private final transient boolean isShowInApp;
    /**
     * This map contains key-value pairs about meta information of this sensor.
     */
    private final HashMap<String, String> metaInfo;
    private String icon;

    /**
     * True if this sensor can be toggled on or off.
     */
    private boolean switchable = false;
    /**
     * This field contains information about the rules this sensor violates.
     */
    private Set<RuleInfo> violatedRules = new HashSet<>();
    /**
     * This field contains information about the rules this sensor passes.
     */
    private Set<RuleInfo> passedRules = new HashSet<>();

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
        if (log.isSwitchable()) {
            switchable = true;
        }
        fileLogs.add(log);
    }

    public String getName() {
        return name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Set<RuleInfo> getViolatedRules() {
        return violatedRules;
    }

    public Set<RuleInfo> getPassedRules() {
        return passedRules;
    }

    public Optional<FHEMFileLog> getLogByName(String filelogName) {
        for (FHEMFileLog log : fileLogs) {
            if (log.getName().equals(filelogName)) {
                return Optional.of(log);
            }
        }
        return Optional.empty();
    }

    /**
     * This method adds a ruleInfo to this sensor.
     *
     * Old rule information is overwritten.
     * This works because the {@link webserver.stateCheck.rules.RuleInfo#equals(Object) equals}
     * and {@link webserver.stateCheck.rules.RuleInfo#hashCode hashCode} methods of RuleInfo only care about the name
     *
     * @param ruleInfo the information which should be added to the set of ruleinfos of this sensor
     */
    public void addViolatedRule(RuleInfo ruleInfo) {
        /* First remove the old info, then put the current one */
        if (violatedRules.contains(ruleInfo)) {
            violatedRules.remove(ruleInfo);
            violatedRules.add(ruleInfo);
        } else {
            violatedRules.add(ruleInfo);
        }
    }

    /**
     * This method adds a ruleInfo to this sensor.
     *
     * Old rule information is overwritten.
     * This works because the {@link webserver.stateCheck.rules.RuleInfo#equals(Object) equals}
     * and {@link webserver.stateCheck.rules.RuleInfo#hashCode hashCode} methods of RuleInfo only care about the name
     *
     * @param ruleInfo the information which should be added to the set of ruleinfos of this sensor
     */
    public void addPassedRule(RuleInfo ruleInfo) {
        /* First remove the old info, then put the current one */
        if (passedRules.contains(ruleInfo)) {
            passedRules.remove(ruleInfo);
            passedRules.add(ruleInfo);
        } else {
            passedRules.add(ruleInfo);
        }
    }

    /**
     * Returns whether any of the logs are permitted to be accessed with the given permissions.
     *
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

    /**
     * This method returns the value of the given field in the metaInfo map or Optional.empty, if
     * such a value was not put in the map.
     *
     * @param field the name of the field in the map
     * @return the value corresponding to the input key
     */
    public Optional<String> getValueOfField(String field) {
        return Optional.ofNullable(metaInfo.get(field));
    }

    public HashSet<FHEMFileLog> getLogs() {
        return fileLogs;
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     */
    public void forEach(Consumer<? super FHEMFileLog> action) {
        fileLogs.forEach(action);
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

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        webserver.fhemParser.fhemModel.sensors.FHEMSensor that = (webserver.fhemParser.fhemModel.sensors.FHEMSensor) o;

        return name.equals(that.name);
    }

    @Override
    public String toString() {
        return "FHEMSensor{"
                + "name='" + name + '\''
                + ", nameInApp='" + nameInApp + '\''
                + ", coords=" + coords
                + ", switchable=" + switchable
                + '}';
    }
}
