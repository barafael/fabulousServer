package webserver.ruleCheck.rules;

import com.google.gson.annotations.SerializedName;

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
    @SerializedName("name")
    private final String ruleName;

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
     * @param ruleName        Name of the rule as shown in app
     * @param isOk            state of the rule; true if ok
     * @param permissions     necessary permissions
     * @param message         the message about the state of the rule
     * @param relatedLogNames the set of names of related logfiles
     * @param priority        the priority of this rule, higher number means higher priority
     */
    public RuleInfo(String ruleName,
                    boolean isOk,
                    Set<String> permissions,
                    String message,
                    Set<String> relatedLogNames,
                    int priority) {
        this.ruleName = ruleName;
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

    public String getRuleName() {
        return ruleName;
    }

    public boolean isOk() {
        return isOk;
    }

    /**
     * This rule is permitted to view if it's permissions is contained in the given permissions.
     *
     * @param callerGroups the caller's permissions
     * @return whether the info is permitted
     */
    public boolean isPermittedForGroups(Collection<String> callerGroups) {
        for (String permission : callerGroups) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the state to ok.
     */
    public void setOk() {
        isOk = true;
    }

    /**
     * Set the state to not ok.
     */
    public void setNotOk() {
        isOk = false;
    }

    @Override
    public int hashCode() {
        return ruleName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleInfo ruleInfo = (RuleInfo) o;

        return ruleName.equals(ruleInfo.ruleName);
    }

    @Override
    public String toString() {
        return "RuleInfo{"
                + "ruleName='"
                + ruleName
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
