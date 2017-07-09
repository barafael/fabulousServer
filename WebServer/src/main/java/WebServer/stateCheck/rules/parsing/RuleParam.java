package WebServer.stateCheck.rules.parsing;

import WebServer.stateCheck.WARNINGLEVEL;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
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

    public Set<String> getRequiredTrueRules() {
        return requiredTrueRules;
    }

    public Set<String> getRequiredFalseRules() {
        return requiredFalseRules;
    }
}
