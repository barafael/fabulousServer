package webserver.ruleCheck.parsing;

import com.google.gson.annotations.SerializedName;
import webserver.ruleCheck.rules.Rule;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class contains all parameters for building a Rule.
 * It should be deserialized from a rules file.
 * All the getters from this class never return null. When they are not set in the corresponding entry
 * in the rules file, they are set to a standard value.
 *
 * See documentation of rule checker for a description of all the single arguments.
 *
 * @author Rafael on 07.07.17.
 */
public final class RuleParam {
    @SerializedName("Name")
    private final String name;
    @SerializedName("Expression")
    private final String expression;
    @SerializedName("ViewPermissions")
    private final Set<String> viewPermissions;
    @SerializedName("SensorNames")
    private final Set<String> sensorNames;
    @SerializedName("OkMessage")
    private final String okMessage;
    @SerializedName("RequiredTrueRules")
    private final Set<String> requiredTrueRules;
    @SerializedName("RequiredFalseRules")
    private final Set<String> requiredFalseRules;
    @SerializedName("ErrorMessages")
    private final Map<Long, String> errorMessages = new HashMap<>();
    @SerializedName("Escalation")
    private final Map<Long, Set<String>> escalation = new HashMap<>();
    @SerializedName("Invisible")
    private boolean invisibleInApp = false;
    /* Must always be sorted after the natural order of keys, therefore TreeSet */
    @SerializedName("RelatedFileLogs")
    private final Set<String> relatedFileLogNames = new HashSet<>();
    @SerializedName("Priority")
    private final int priority;

    public RuleParam(String name,
                     Set<String> sensorNames,
                     Set<String> viewPermissions,
                     String expression,
                     Set<String> requiredTrueRules,
                     Set<String> requiredFalseRules,
                     String okMessage,
                     Map<Long, String> errorMessages,
                     Map<Long, Set<String>> escalation,
                     boolean invisibleInApp,
                     Set<String> relatedFileLogNames,
                     int priority) {
        this.name = name;
        this.sensorNames = sensorNames;
        this.viewPermissions = viewPermissions;
        this.expression = expression;
        this.requiredTrueRules = requiredTrueRules;
        this.requiredFalseRules = requiredFalseRules;
        this.okMessage = okMessage;
        this.errorMessages.putAll(errorMessages);
        this.escalation.putAll(escalation);
        this.invisibleInApp = invisibleInApp;
        this.relatedFileLogNames.addAll(relatedFileLogNames);
        this.priority = priority;
    }

    /**
     * The type of the rule is deduced from the provided expression.
     * @return the type of the rule
     */
    public RuleType getType() {

        if (expression == null || expression.isEmpty()) {
            return RuleType.META;
        }

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
        return name != null ? name : "Nameless Rule";
    }

    public Set<String> getViewPermissions() {
        return viewPermissions != null ? viewPermissions : new HashSet<>();
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

    public Map<Long, String> getErrorMessages() {
        if (errorMessages == null || errorMessages.isEmpty()) {
            HashMap<Long, String> map = new HashMap<>();
            map.put(0L, "No error messages defined!");
            return map;
        }
        return errorMessages;
    }

    public Map<Long, Set<String>> getEscalation() {
        if (escalation == null || escalation.isEmpty()) {
            HashMap<Long, Set<String>> map = new HashMap<>();
            map.put(0L, new HashSet<>(Collections.singletonList("")));
            return map;
        }
        return escalation;
    }

    public boolean getInvisible() {
        return invisibleInApp;
    }

    public Set<String> getRelatedLogs() {
        return relatedFileLogNames == null ? new HashSet<>() : relatedFileLogNames;
    }

    public int getPriority() {
        return priority;
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
