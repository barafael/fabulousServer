package webserver.fhemParser.fhemModel.sensors;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import webserver.fhemParser.fhemModel.log.FHEMFileLog;
import webserver.ruleCheck.rules.RuleInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This class represents the data about a sensor gathered from FHEM.
 * It also contains the FileLog stubs needed to get timeseries.
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
    @SerializedName("alias")
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
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final transient List<String> permissions;
    /**
     * All filelogs which belong to this sensor.
     */
    private final HashSet<FHEMFileLog> fileLogs = new HashSet<>();
    /**
     * This map contains key-value pairs about meta information of this sensor.
     */
    private final HashMap<String, String> metaInfo;
    /**
     * This field contains information about the rules this sensor violates.
     */
    private final Set<RuleInfo> violatedRules = new HashSet<>();
    /**
     * This field contains information about the rules this sensor passes.
     */
    private final Set<RuleInfo> passedRules = new HashSet<>();

    /**
     * Whether this sensor should be shown in the application.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean isVisibleInApp;
    /**
     * The icon name of this sensor, which will be deserialized.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private String icon;
    /**
     * True if this sensor can be toggled on or off.
     */
    private boolean switchable = false;

    /**
     * Construct a FHEMSensor.
     * @param coordX the x coordinate in percent
     * @param coordY the y coordinate in percent
     * @param name the name of the sensor in FHEM
     * @param nameInApp the name of the sensor in the app, should be the FHEM alias
     * @param permissions the permissions of this sensor, read from FHEM
     * @param isVisibleInApp whether this sensor should be visible in the app
     * @param metaInfo meta information like readings and custom variables which are useful for display
     */
    public FHEMSensor(int coordX, int coordY, String name, String nameInApp, List<String> permissions,
                      boolean isVisibleInApp, HashMap<String, String> metaInfo) {
        this.coords = new Coordinates(coordX, coordY);
        this.name = name;
        this.nameInApp = nameInApp;
        this.permissions = permissions;
        this.isVisibleInApp = isVisibleInApp;
        this.metaInfo = metaInfo;
    }

    /**
     * Add meta information to this sensor.
     * @param key the variable's name
     * @param value the value to set it to
     */
    public void addMeta(@NotNull String key, @NotNull String value) {
        metaInfo.put(key, value);
    }

    /**
     * Add a log to this sensor.
     * @param log the log to add
     */
    public void addLog(FHEMFileLog log) {
        if (log.isSwitchable()) {
            switchable = true;
        }
        fileLogs.add(log);
    }

    public String getName() {
        return name;
    }

    /**
     * Set the icon of this sensor (as name of icon in the app).
     * @param icon the name of the icon
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Access the violated rules of this sensor as ruleinfo.
     * @return the violated rules' information
     */
    public Set<RuleInfo> getViolatedRules() {
        return violatedRules;
    }

    /**
     * Access the violated rules of this sensor as ruleinfo.
     * @return the violated rules' information
     */
    public Set<RuleInfo> getPassedRules() {
        return passedRules;
    }

    /**
     * Get all rule infos, whether passed or violated.
     * @return a set of all ruleinfos
     */
    public Set<RuleInfo> getRuleInfos() {
        Set<RuleInfo> all = new HashSet<>(passedRules);
        all.addAll(violatedRules);
        return all;
    }

    /**
     * Get a log of this sensor by name.
     * @param filelogName the name of the log
     * @return the filelog of this name, or empty if not present
     */
    public Optional<FHEMFileLog> getLogByName(String filelogName) {
        for (FHEMFileLog log : fileLogs) {
            if (log.getName().equals(filelogName)) {
                return Optional.of(log);
            }
        }
        return Optional.empty();
    }

    /**
     * This method adds a set of ruleInfos to this sensor.
     * It replaces whichever infos were there before.
     *
     * @param ruleInfos the set of ruleInfos which will replace the previous one.
     */
    public void addRuleInfos(Collection<RuleInfo> ruleInfos) {
        passedRules.clear();
        violatedRules.clear();
        for (RuleInfo ruleInfo : ruleInfos) {
            if (ruleInfo.isOk()) {
                passedRules.add(ruleInfo);
            } else {
                violatedRules.add(ruleInfo);
            }
        }
    }

    /**
     * This device is switchable if it contains 'switchable' logs.
     * @return if this device can be switched on or off
     */
    public boolean isSwitchable() {
        return switchable;
    }

    /**
     * A sensor is a permitted switch if permissions suffice and it is switchable.
     * @param permissions the caller's permissions
     * @return whether this device can be switched on and off
     */
    public boolean isPermittedSwitch(List<String> permissions) {
        for (FHEMFileLog log : this) {
            if (log.isPermittedSwitch(permissions)) {
                return true;
            }
        }
        return false;
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

    /**
     * Returns whether any of the rules are permitted to be accessed with the given permissions.
     *
     * @param permissions list of permissions against which to check
     * @return whether this sensor contains viewable rules
     */
    public boolean hasPermittedRules(List<String> permissions) {
        for (RuleInfo info : passedRules) {
            if (info.isPermitted(permissions)) {
                return true;
            }
        }
        for (RuleInfo info : violatedRules) {
            if (info.isPermitted(permissions)) {
                return true;
            }
        }
        return false;
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

    /**
     * Get all logs of this sensor.
     * @return a set of logs which contain data about this sensor
     */
    public HashSet<FHEMFileLog> getLogs() {
        return fileLogs;
    }

    /**
     * An example predicate for use with the rule checker. See testSensorPredicate() for usage.
     * (Not linkable from here because it is a unit test).
     * It is not unused, but since it is called at runtime via reflection, it cannot be detected at compile time.
     * <p>
     * Predicates must be public, return boolean, and take a List<String> (even if they ignore it).
     *
     * @param arguments the strings defined in the input file
     * @return true, always (to make it more useful, make it return something based on the sensor status)
     */
    @SuppressWarnings({"unused", "SameReturnValue"})
    public boolean exampleAlwaysTruePredicate(List<String> arguments) {
        arguments.forEach(s -> System.out.print(s + " "));
        System.out.println();
        return true;
    }

    /**
     * This method is necessary to be able to iterate over an internal data structure.
     * @param action the consumer for each element of the iteration
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

        FHEMSensor that = (webserver.fhemParser.fhemModel.sensors.FHEMSensor) o;

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
