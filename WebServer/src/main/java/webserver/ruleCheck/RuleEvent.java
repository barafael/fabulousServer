package webserver.ruleCheck;

import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleState;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * This class represents a state of a rule in the past, marked by start and end date.
 * @author Rafael
 */

public final class RuleEvent {
    /**
     * The message for this rule, to be shown in the frontend.
     */
    private String message;
    /**
     * The time of violation at which this event started.
     */
    private long startTime;
    /**
     * The time at which the rule violation was resolved.
     */
    private long endTime;
    /**
     * The names of the sensors which were violating the rule in the end.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private Set<String> sensors;
    /**
     * The name of the violated rule.
     */
    private String ruleName;
    /**
     * The groups to which this event should be shown.
     */
    private transient Set<String> groups;

    /**
     * Private constructor which constructs a ruleevent field-by-field.
     * Used by the other constructor in this class only.
     * @param message the message of this event
     * @param startTime the start time in unix timestamps
     * @param endTime the end time in unix timestamps
     * @param sensors a list of involved sensors
     * @param ruleName the name of the violated rule
     * @param groups the groups for which this event should be shown
     */
    private RuleEvent(String message,
                      long startTime,
                      long endTime,
                      Set<String> sensors,
                      String ruleName,
                      Set<String> groups) {
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sensors = sensors;
        this.ruleName = ruleName;
        this.groups = groups;
    }

    /**
     * Construct an event from a state and a corresponding rule
     * @param state the state, from which timestamps and violating sensors are queried
     * @param rule the violated rule, from which the name and viewgroups are queried
     */
    RuleEvent(RuleState state, Rule rule) {
        this(
                rule.getWarningMessage(state.getLastStamp()),
                state.getLastStamp(),
                Instant.now().getEpochSecond(),
                state.getViolatedSensors(),
                rule.getName(),
                rule.getAllViewGroups()
        );
    }

    long getStartTime() {
        return startTime;
    }

    /**
     * Check whether the callerGroups set for this event permit it to be shown to a
     * user in the given callerGroups.
     *
     * @param callerGroups the callerGroups of the caller
     * @return true if the permissions of the caller suffice for this event to be shown to them
     */
    public boolean isPermittedForGroups(List<String> callerGroups) {
        for (String group : this.groups) {
            if (callerGroups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Rule " + ruleName + " starting at "
                + startTime + " for "
                + ((endTime - startTime) / 60L) + " minutes: " + message;
    }
}
