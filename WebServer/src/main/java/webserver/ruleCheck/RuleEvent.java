package webserver.ruleCheck;

import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleState;

import java.time.Instant;
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

    public RuleEvent(String message, long startTime, long endTime, Set<String> sensors, String ruleName) {
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sensors = sensors;
        this.ruleName = ruleName;
    }

    public static RuleEvent fromState(RuleState state, Rule rule) {
        return new RuleEvent(
                rule.getWarningMessage(state.getLastStamp()),
                state.getLastStamp(),
                Instant.now().getEpochSecond(),
                state.getViolatedSensors(),
                rule.getName());
    }

    @Override
    public String toString() {
        return "Rule " + ruleName + " starting at " +
                startTime + " for " +
                ((endTime - startTime) / 60L) + " minutes: " + message;
    }
}
