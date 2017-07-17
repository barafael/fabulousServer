package webserver.ruleCheck.parsing;

import com.google.gson.annotations.SerializedName;
import webserver.ruleCheck.WARNINGLEVEL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    @SerializedName("Name")
    private final String name;
    @SerializedName("SensorNames")
    private final Set<String> sensorNames;
    @SerializedName("Permission")
    private final String permission;
    @SerializedName("Expression")
    private final String expression;
    @SerializedName("RequiredTrueRules")
    private final Set<String> requiredTrueRules;
    @SerializedName("RequiredFalseRules")
    private final Set<String> requiredFalseRules;
    @SerializedName("OkMessage")
    private final String okMessage;
    @SerializedName("ErrorMessages")
    private final Map<WARNINGLEVEL, String> errorMessages;
    @SerializedName("IsVisibleInApp")
    private final boolean isVisibleInApp;
    /* Must always be sorted after the natural order of keys, therefore TreeSet */
    @SerializedName("Escalation")
    private Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();
    @SerializedName("RelevantFileLogs")
    private final Set<String> relevantFileLogNames = new HashSet<>();

    public RuleParam(String name,
                     Set<String> sensorNames,
                     String permission,
                     String expression,
                     Set<String> requiredTrueRules,
                     Set<String> requiredFalseRules,
                     String okMessage, Map<WARNINGLEVEL, String> errorMessages,
                     boolean isVisibleInApp,
                     Map<Long, WARNINGLEVEL> escalation) {
        this.name = name;
        this.sensorNames = sensorNames;
        this.permission = permission;
        this.expression = expression;
        this.requiredTrueRules = requiredTrueRules;
        this.requiredFalseRules = requiredFalseRules;
        this.okMessage = okMessage;
        this.errorMessages = errorMessages;
        this.isVisibleInApp = isVisibleInApp;
        this.escalation = escalation;
    }

    public RuleType getType() {
        String[] tokens = expression.split(" ");

        if (tokens.length == 3) {
            if ("!<=>".contains(tokens[1])) {
                return RuleType.NUMERIC;
            } else {
                if (tokens[1].startsWith("not")) {
                    tokens[1] = tokens[1].substring(3);
                }
                if ("equalsmatchescontains".contains(tokens[1])) {
                    return RuleType.REGEXP;
                }
            }
        }
        if (expression.startsWith("Sensor ") && expression.length() > 7) {
            return RuleType.SENSOR_PRED;
        }
        if (expression.startsWith("Predicate ") && expression.length() > 10) {
            return RuleType.GENERAL_PRED;
        }
        return RuleType.UNKNOWN;
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

    public boolean getVisible() {
        return isVisibleInApp;
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

    @Override
    public String toString() {
        return "RuleParam{"
                + "name='" + name + '\''
                + ", sensorNames=" + sensorNames
                + ", expression='" + expression + '\''
                + ", requiredTrueRules=" + requiredTrueRules
                + ", requiredFalseRules=" + requiredFalseRules
                + '}';
    }
}
