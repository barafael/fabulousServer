package webserver.stateCheck.rules;

import java.util.List;

/**
 * A class containing information about a rule.
 * This cannot be solved easily by marking fields in Rule as transient,
 * because they have to be deserialized.
 *
 * @author Rafael
 */

public final class RuleInfo {
    /**
     * The name of the Rule as shown in the frontend.
     */
    private String name;

    /**
     * The state of this rule. True if not violated.
     */
    @SuppressWarnings ("FieldCanBeLocal")
    private boolean isOk;
    /* TODO handle permissions similarly to filelogs */
    /**
     * The necessary permissions to be able to see this information in a sensor.
     */
    private transient String permission;
    /**
     * A message about the state of the rule.
     * This field is needed for serialization
     */
    @SuppressWarnings ("FieldCanBeLocal")
    private String message;

    /**
     * Construct a RuleInfo instance.
     *
     * @param name       Name of the rule as shown in app
     * @param isOk       state of the rule; true if ok
     * @param permission necessary permissions
     * @param message    the message about the state of the rule
     */
    public RuleInfo(String name, boolean isOk, String permission, String message) {
        this.name = name;
        this.isOk = isOk;
        this.permission = permission;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public boolean isOk() {
        return isOk;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPermitted(List<String> permissions) {
        return permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "RuleInfo{"
                + "name='" + name + '\''
                + ", isOk=" + isOk
                + ", permission='" + permission + '\''
                + ", message='" + message + '\''
                + '}';
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
}
