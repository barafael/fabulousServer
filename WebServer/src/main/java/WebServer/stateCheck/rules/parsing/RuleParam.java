package WebServer.stateCheck.rules.parsing;

import WebServer.stateCheck.WARNINGLEVEL;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class contains all parameters for building a Rule.
 * It should be deserialized from a rules file.
 *
 * @author Rafael on 07.07.17.
 */
public class RuleParam {
    String name;
    Set<String> sensorNames;
    RuleType ruleType;
    String permission;
    String expression;
    Set<String> requiredTrueRules;
    Set<String> requiredFalseRules;
    String okMessage;
    Map<WARNINGLEVEL, String> errorMessages;
    /* Must always be sorted after the natural order of keys, therefore TreeSet */
    Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();

    private RuleParam() {
    }

    public RuleType getType() {
        return ruleType;
    }

    public String getName() {
        return name;
    }

    public String getPermissionField() {
        return permission;
    }

    public Set<String> getSensorNames() {
        return sensorNames;
    }

    public String getOkMessage() {
        return okMessage;
    }

    /**
     * Returns requiredTrue-Rules, which are rules which must be true as a pre-requisite
     * @return a set of rules which are required to be true
     */
    public Set<String> getRequiredTrueRules() {
        return requiredTrueRules;
    }

    /**
     * Returns requiredFalse-Rules, which are rules which must be false as a pre-requisite
     * @return a set of rules which are required to be false
     */
    public Set<String> getRequiredFalseRules() {
        return requiredFalseRules;
    }

    public String getExpression() {
        return expression;
    }

    public Map<WARNINGLEVEL, String> getErrorMessages() {
        return errorMessages;
    }

    public Map<Long, WARNINGLEVEL> getEscalation() {
        return escalation;
    }
}
