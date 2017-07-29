package webserver.ruleCheck;

import com.google.gson.annotations.SerializedName;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleState;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class holds information about the state of a violated rule at one moment.
 *
 * @author Rafael
 */
public final class RuleSnapshot {
    /**
     * The rule for which this state applies.
     */
    @SerializedName("name")
    private final String ruleName;

    /**
     * The latest message of this rule.
     */
    private String message;

    private boolean important = false;
    private int priority = 0;

    /**
     * The current escalation permissions.
     */
    private Set<String> escalationPermissions = new HashSet<>();

    /**
     * A set of sensors for which this rule does not hold.
     */
    private final Set<String> violatedSensors;

    /**
     * The unix timestamp of the latest change.
     */
    private long stamp;


    public RuleSnapshot(RuleState state, Rule rule) {
        this.ruleName = state.getRuleName();
        this.violatedSensors = state.getViolatedSensors();
        this.stamp = state.getLastStamp();
        message = rule.getWarningMessage(stamp);
        escalationPermissions = rule.getEscalationLevelViewGroups(stamp);
        this.priority = rule.getPriority();
        this.important = rule.isImportant();
    }

    public Set<String> getViolatedSensors() {
        return violatedSensors;
    }

    public String getRuleName() {
        return ruleName;
    }

    public long getDuration() {
        return Instant.now().getEpochSecond() - stamp;
    }

    public long getLastStamp() {
        return stamp;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * This rule is permitted to view if it's permissions is contained in the given permissions.
     *
     * @param callerPermissions the caller's permissions
     * @return whether the info is permitted
     */
    public boolean isPermittedForGroups(Collection<String> callerPermissions) {
        for (String permission : callerPermissions) {
            if (this.escalationPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
}
