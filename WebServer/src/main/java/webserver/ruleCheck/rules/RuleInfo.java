package webserver.ruleCheck.rules;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A class containing information about a rule.
 *
 * @author Rafael
 */

public final class RuleInfo {
    /**
     * The name of the Rule as shown in the frontend.
     */
    private final String name;

    /**
     * The state of this rule. False if violated.
     */
    private boolean isOk;

    /**
     * How many changes of state will be recorded until the oldest is discarded.
     */
    private static final int CHANGE_THRESHHOLD = 15;

    /**
     * The necessary permissions to be able to see this information in a sensor.
     */
    private final transient String permission;

    /**
     * A message about the state of the rule.
     */
    private String message;

    private final Set<String> relatedLogNames = new HashSet<>();

    private final Map<Long, Boolean> changeStamps = new TreeMap<>();

    /**
     * Construct a RuleInfo instance.
     *
     * @param name       Name of the rule as shown in app
     * @param isOk       state of the rule; true if ok
     * @param permission necessary permissions
     * @param message    the message about the state of the rule
     */
    public RuleInfo(String name,
                    boolean isOk,
                    String permission,
                    String message,
                    Set<String> relatedLogNames) {
        this.name = name;
        this.isOk = isOk;
        this.permission = permission;
        this.message = message;
        this.relatedLogNames.addAll(relatedLogNames);

        changeStamps.put(Instant.now().getEpochSecond(), isOk);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public boolean isOk() {
        return isOk;
    }

    public boolean isPermitted(List<String> permissions) {
        return permissions.contains(permission);
    }

    public void setOk() {
        if (isOk) {
            return;
        }
        isOk = true;
        changeStamps.put(Instant.now().getEpochSecond(), true);

        /* TODO: exploit TreeMap */
        if (changeStamps.size() > CHANGE_THRESHHOLD) {
            Set<Long> timestamps = changeStamps.keySet();
            timestamps.removeIf((Long l) -> l.longValue() == Collections.min(timestamps).longValue());
        }
    }

    public void setNotOk() {
        if (!isOk) {
            return;
        }
        isOk = false;
        changeStamps.put(Instant.now().getEpochSecond(), false);

        /* TODO: exploit TreeMap */
        if (changeStamps.size() > CHANGE_THRESHHOLD) {
            Set<Long> timestamps = changeStamps.keySet();
            timestamps.removeIf((Long l) -> l.longValue() == Collections.min(timestamps).longValue());
        }
    }

    @Override
    public String toString() {
        return "RuleInfo{" +
                "name='" + name + '\'' +
                ", isOk=" + isOk +
                ", permission='" + permission + '\'' +
                ", message='" + message + '\'' +
                ", changeStamps=" + changeStamps +
                '}';
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleInfo ruleInfo = (RuleInfo) o;

        return name.equals(ruleInfo.name);
    }

    public long getLastStamp() {
        return Collections.max(changeStamps.keySet());
    }
}
