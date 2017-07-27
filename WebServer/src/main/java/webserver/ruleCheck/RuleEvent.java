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

    static RuleEvent fromState(RuleState state, Rule rule) {
        return new RuleEvent(
                rule.getWarningMessage(state.getLastStamp()),
                state.getLastStamp(),
                Instant.now().getEpochSecond(),
                state.getViolatedSensors(),
                rule.getName(),
                rule.getGroups());
    }

    @Override
    public String toString() {
        return "Rule " + ruleName + " starting at " +
                startTime + " for " +
                ((endTime - startTime) / 60L) + " minutes: " + message;
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
}
