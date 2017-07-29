package webserver.ruleCheck;

import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleState;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @author Rafael
 */

public class RuleEvent {
    private String message;
    private long startTime;
    private long endTime;
    @SuppressWarnings("FieldCanBeLocal")
    private Set<String> sensors;
    private String ruleName;
    private transient Set<String> groups;

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

    public boolean isPermittedForGroups(List<String> groups) {
        for (String group : this.groups) {
            if (groups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Rule " + ruleName + " starting at " +
                startTime + " for " +
                ((endTime - startTime) / 60L) + " minutes: " + message;
    }
}
