package webserver.ruleCheck.parsing;

import com.google.gson.annotations.SerializedName;

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
 * <p>
 * See documentation of rule checker for a description of all the single arguments.
 *
 * @author Rafael on 07.07.17.
 */
public final class RuleParam {
    /**
     * The name of the rule.
     */
    @SerializedName("Name")
    private final String name;
    /**
     * The expression to be evaluated. If the expression is true, the rule is true for the given scope
     * (global, sensor).
     */
    @SerializedName("Expression")
    private final String expression;
    /**
     * The groups which can see this rule and representations of it.
     */
    @SerializedName("ViewPermissions")
    private Set<String> viewPermissions;
    /**
     * The FHEM names of the sensors for which this rule should be evaluated.
     */
    @SerializedName("SensorNames")
    private final Set<String> sensorNames;
    /**
     * The message to display if a rule is not violated.
     */
    @SerializedName("OkMessage")
    private final String okMessage;
    /**
     * The set of rules of which all must be true for this rule to be evaluated to true.
     */
    @SerializedName("RequiredAllTrue")
    private final Set<String> andRules;
    /**
     * The set of rules of which one must be true for this rule to be evaluated to true.
     */
    @SerializedName("RequiredOneOfTrue")
    private final Set<String> orRules;
    /**
     * The error messages for this rule, sorted by the time after which they should be shown.
     */
    @SerializedName("ErrorMessages")
    private final Map<Long, String> errorMessages = new HashMap<>();
    /**
     * The escalation levels of this rule, sorted by the time after which they are reached.
     */
    @SerializedName("Escalation")
    private Map<Long, Set<String>> escalation = new HashMap<>();
    /**
     * A rule can be marked invisible to be evaluated as precondition for another rule but not be shown in the app.
     */
    @SerializedName("Invisible")
    private boolean invisibleInApp = false;
    /**
     * The rule definition can include related filelogs, which currently are unused by the frontend.
     */
    @SerializedName("RelatedFileLogs")
    private final Set<String> relatedFileLogNames = new HashSet<>();
    /**
     * The priority of the defined rule, higher means it will be shown more likely in the frontend.
     */
    @SerializedName("Priority")
    private final int priority;
    /**
     * If a rule is marked as important, it is shown on the main screen if violated.
     */
    @SerializedName("Important")
    private final boolean important;

    /**
     * Constructor for unit tests only.
     * Real construction happens from gson.
     *
     * @param name the name of the rule
     * @param sensorNames the names of sensors
     * @param viewPermissions permissions to see this rule
     * @param expression the rule's expression
     * @param andRules all have to be true
     * @param orRules one has to be true
     * @param okMessage the ok message
     * @param errorMessages the error messages map
     * @param escalation a map of time to escalation levels
     * @param invisibleInApp if this rule should be shown in the frontend
     * @param relatedFileLogNames names of related logfiles
     * @param priority the priority of the defined rule
     * @param important if this rule should be shown in the main screen
     */
    public RuleParam(String name,
                     Set<String> sensorNames,
                     Set<String> viewPermissions,
                     String expression,
                     Set<String> andRules,
                     Set<String> orRules,
                     String okMessage,
                     Map<Long, String> errorMessages,
                     Map<Long, Set<String>> escalation,
                     boolean invisibleInApp,
                     Set<String> relatedFileLogNames,
                     int priority,
                     boolean important) {
        this.name = name;
        this.sensorNames = sensorNames;
        this.viewPermissions = viewPermissions;
        this.expression = expression;
        this.andRules = andRules;
        this.orRules = orRules;
        this.okMessage = okMessage;
        this.errorMessages.putAll(errorMessages);
        this.escalation.putAll(escalation);
        this.invisibleInApp = invisibleInApp;
        this.relatedFileLogNames.addAll(relatedFileLogNames);
        this.priority = priority;
        this.important = important;
    }

    /**
     * The type of the rule is deduced from the provided expression.
     *
     * @return the type of the rule
     */
    public RuleType getRuleType() {

        if (expression == null || expression.isEmpty()) {
            return RuleType.META;
        }

        String[] tokens = expression.split("\\s+");

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

    /**
     * The view permissions are the union of the permissions in the escalation and the defined viewpermissions.
     * This means that anyone who will ever be notified can also see the rule represented,
     * as can anyone who has view permissions (but will not be notified).
     *
     * @return the permissions necessary to see this rule
     */
    public Set<String> getViewPermissions() {
        if (viewPermissions == null) {
            viewPermissions = new HashSet<>();
        }
        if (escalation == null) {
            escalation = new HashMap<>();
        }
        for (Map.Entry<Long, Set<String>> longSetEntry : escalation.entrySet()) {
            viewPermissions.addAll(longSetEntry.getValue());
        }
        return viewPermissions;
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
    public Set<String> getAndRules() {
        return andRules != null ? andRules : new HashSet<>();
    }

    /**
     * Returns requiredFalse-Rules, which are rules which must be false as a pre-requisite.
     *
     * @return a set of rules which are required to be false
     */
    public Set<String> getOrRules() {
        return orRules != null ? orRules : new HashSet<>();
    }

    public String getExpression() {
        return expression != null ? expression : "";
    }

    /**
     * Get the errormessages from the rule definition.
     * @return a map of seconds to error messages
     */
    public Map<Long, String> getErrorMessages() {
        if (errorMessages == null || errorMessages.isEmpty()) {
            HashMap<Long, String> map = new HashMap<>();
            map.put(0L, "No error messages defined!");
            return map;
        }
        return errorMessages;
    }

    /**
     * Get the escalation map.
     * @return a map of elapsed time to a set of groups.
     */
    public Map<Long, Set<String>> getEscalation() {
        if (escalation == null || escalation.isEmpty()) {
            HashMap<Long, Set<String>> map = new HashMap<>();
            map.put(0L, new HashSet<>(Collections.singletonList("")));
            return map;
        }
        return escalation;
    }

    /**
     * A rule can be marked invisible.
     * @return true if this rule is marked invisible
     */
    public boolean getInvisible() {
        return invisibleInApp;
    }

    public Set<String> getRelatedLogs() {
        return relatedFileLogNames == null ? new HashSet<>() : relatedFileLogNames;
    }

    public boolean isImportant() {
        return important;
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
                + ", andRules=" + andRules
                + ", requiredFalseRules=" + orRules
                + '}';
    }
}
