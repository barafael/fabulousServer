package webserver.ruleCheck.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
     * The necessary permissions to be able to see this information in a sensor.
     */
    private final transient Set<String> permissions;
    /**
     * The names of logs which the corresponding rule should affect.
     */
    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private final Set<String> relatedLogNames = new HashSet<>();
    /**
     * The priority this rule has over others.
     * (Used through deserialization only)
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final int priority;
    /**
     * The state of this rule. False if violated.
     */
    private boolean isOk;
    /**
     * A message about the state of the rule.
     */
    private String message = "";
    /**
     * Record whether a new message has been set.
     * In this case, a notification should appear.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private boolean hasNewMessage;

    /**
     * Construct a RuleInfo instance.
     *
     * @param name            Name of the rule as shown in app
     * @param isOk            state of the rule; true if ok
     * @param permissions     necessary permissions
     * @param message         the message about the state of the rule
     * @param relatedLogNames the set of names of related logfiles
     * @param priority        the priority of this rule, higher number means higher priority
     */
    public RuleInfo(String name,
                    boolean isOk,
                    Set<String> permissions,
                    String message,
                    Set<String> relatedLogNames,
                    int priority) {
        this.name = name;
        this.isOk = isOk;
        this.permissions = permissions;
        this.message = message != null ? message : "";
        this.relatedLogNames.addAll(relatedLogNames);
        this.priority = priority;
    }

    /**
     * Set the current message. If there is a change to the old message, hasNewMessage is true.
     *
     * @param message the new or old message
     */
    public void setMessage(String message) {
        this.hasNewMessage = !this.message.equals(message);
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public boolean isOk() {
        return isOk;
    }

    /**
     * This rule is permitted to view if it's permissions is contained in the given permissions.
     *
     * @param callerPermissions the caller's permissions
     * @return whether the info is permitted
     */
    public boolean isPermitted(Collection<String> callerPermissions) {
        for (String permission : callerPermissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the state to ok.
     * If the state is already ok, then the set is ignored.
     * If the state was not ok, the changestamps are updated.
     * The changestamps are never more than CHANGE_THRESHHOLD.
     */
    public void setOk() {
        if (isOk) {
            return;
        }
        isOk = true;
    }

    /**
     * Set the state to not ok.
     * If the state is already not ok, then the set is ignored.
     * If the state was ok, the changestamps are updated.
     * The changestamps are never more than CHANGE_THRESHHOLD.
     */
    public void setNotOk() {
        if (!isOk) {
            return;
        }
        isOk = false;
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

    @Override
    public String toString() {
        return "RuleInfo{"
                + "name='"
                + name
                + '\''
                + ", isOk="
                + isOk
                + ", permissions='"
                + permissions + '\''
                + ", message='"
                + message + '\''
                + '}';
    }
}
