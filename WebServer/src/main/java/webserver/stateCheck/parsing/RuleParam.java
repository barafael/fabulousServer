package webserver.stateCheck.parsing;

import webserver.stateCheck.WARNINGLEVEL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class contains all parameters for building a Rule.
 * It should be deserialized from a rules file.
 * All the getters from this class never return null. When they are not set in the corresponding entry
 * in the rules file, they are set to a standard value.
 *
 * @author Rafael on 07.07.17.
 */
public final class RuleParam {
    private String name;
    private Set<String> sensorNames;
    private RuleType ruleType;
    private String permission;
    private String expression;
    private Set<String> requiredTrueRules;
    private Set<String> requiredFalseRules;
    private String okMessage;
    private Map<WARNINGLEVEL, String> errorMessages;
    /* Must always be sorted after the natural order of keys, therefore TreeSet */
    private Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();

    public RuleParam(String name,
                     Set<String> sensorNames,
                     RuleType ruleType,
                     String permission,
                     String expression,
                     Set<String> requiredTrueRules,
                     Set<String> requiredFalseRules,
                     String okMessage,
                     Map<WARNINGLEVEL, String> errorMessages,
                     Map<Long, WARNINGLEVEL> escalation) {
        this.name = name;
        this.sensorNames = sensorNames;
        this.ruleType = ruleType;
        this.permission = permission;
        this.expression = expression;
        this.requiredTrueRules = requiredTrueRules;
        this.requiredFalseRules = requiredFalseRules;
        this.okMessage = okMessage;
        this.errorMessages = errorMessages;
        this.escalation = escalation;
    }

    public RuleType getType() {
        return ruleType;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public String getPermissionField() {
        return permission != null ? permission : "";
    }

    public Set<String> getSensorNames() {
        return sensorNames != null ? sensorNames : new HashSet<>();
    }

    public String getOkMessage() {
        return okMessage != null ? okMessage : "";
    }

    /**
     * Returns requiredTrue-Rules, which are rules which must be true as a pre-requisite.
     *
     * @return a set of rules which are required to be true
     */
    public Set<String> getRequiredTrueRules() {
        return requiredTrueRules != null ? requiredTrueRules : new HashSet<>();
    }

    /**
     * Returns requiredFalse-Rules, which are rules which must be false as a pre-requisite.
     *
     * @return a set of rules which are required to be false
     */
    public Set<String> getRequiredFalseRules() {
        return requiredFalseRules != null ? requiredFalseRules : new HashSet<>();
    }

    public String getExpression() {
        return expression != null ? expression : "";
    }

    public Map<WARNINGLEVEL, String> getErrorMessages() {
        return errorMessages != null ? errorMessages : new HashMap<>();
    }

    public Map<Long, WARNINGLEVEL> getEscalation() {
        return escalation != null ? escalation : new HashMap<>();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleParam ruleParam = (RuleParam) o;

        return name.equals(ruleParam.name);
    }
}
