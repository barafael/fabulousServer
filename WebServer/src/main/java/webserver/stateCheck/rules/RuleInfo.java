package webserver.stateCheck.rules;

/**
 * A class containing information about a rule.
 * This cannot be solved easily by marking fields in Rule as transient,
 * because they have to be deserialized.
 *
 * @author Rafael
 */

public class RuleInfo {
    private String name;

    /* TODO handle permissions similarly to filelogs */
    private transient String permission;
    /* This field is needed for serialization */
    @SuppressWarnings("FieldCanBeLocal")
    private String message;

    public RuleInfo(String name, String permission, String message) {
        this.name = name;
        this.permission = permission;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleInfo ruleInfo = (RuleInfo) o;

        return name.equals(ruleInfo.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
