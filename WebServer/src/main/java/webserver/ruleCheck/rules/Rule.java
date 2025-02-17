package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.parsing.RuleParam;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This abstract class contains the attributes of a rule in fhem, most notably the eval(model) method.
 *
 * @author Rafael
 */
public abstract class Rule {
    /**
     * A set of names for which this rule applies.
     * Sensor names should be used (and not aliases) to guarantee uniqueness
     */
    protected final Set<String> sensorNames;
    /**
     * The expression which should be evaluated for each sensor, like 'State matches d*y'.
     */
    protected final String expression;
    /**
     * The name of the rule.
     */
    protected final String name;
    /**
     * The viewPermissions for this rule to be shown.
     * Consists of the given viewpermissions and the permissions defined in the escalation set.
     */
    private final Set<String> viewPermissions;
    /**
     * The 'OK' message.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String okMessage;
    /**
     * A map of durations to their warning levels.
     */
    private Map<Long, String> errorMessages = new TreeMap<>();

    /**
     * A map of duration to permissions which should be notified.
     */
    private final Map<Long, Set<String>> escalation;
    /**
     * A set of related log names.
     */
    private final Set<String> relatedLogNames = new HashSet<>();
    /**
     * Whether the attribute should be applied to model.
     * Reverse logic necessary because gson defaults to false
     * for booleans if a field is not set at all in the input.
     */
    private final boolean invisible;

    /**
     * How important this rule is with regards to other rules.
     * Should be a positive number, with a higher number indicating a higher priority.
     * This is set by the rule parameters.
     */
    private final int priority;

    /**
     * The state of this rule, consisting of a state holder boolean, and sets of sensors which are ok/violated.
     */
    protected RuleState ruleState;

    /**
     * Whether this rule has already been evaluated.
     */
    protected boolean isEvaluated = false;
    /**
     * Required true prerequisites.
     */
    private Set<Rule> andRules;
    /**
     * Required false prerequisites.
     */
    private Set<Rule> orRules;

    /**
     * Whether this rule should be shown in the main screen.
     */
    private boolean important = false;

    /**
     * Construct a rule from rule parameters.
     * If some values in the rule parameters aren't set, default values are used.
     *
     * @param ruleParam the rule parameters
     */
    Rule(RuleParam ruleParam) {
        name = ruleParam.getName();
        sensorNames = ruleParam.getSensorNames();
        expression = ruleParam.getExpression();
        okMessage = ruleParam.getOkMessage();
        invisible = ruleParam.getInvisible();
        relatedLogNames.addAll(ruleParam.getRelatedLogs());
        this.priority = ruleParam.getPriority();
        this.errorMessages = ruleParam.getErrorMessages();
        this.escalation = ruleParam.getEscalation();
        viewPermissions = ruleParam.getViewPermissions();
        important = ruleParam.isImportant();
    }

    public String getName() {
        return name;
    }

    /**
     * Set the rules of which all have to be true.
     *
     * @param requiredTrue the rules to set as true prerequisites
     */
    public void setAndRules(Set<Rule> requiredTrue) {
        this.andRules = requiredTrue;
    }

    /**
     * Set the or rules of which only one has to be true.
     *
     * @param orRules the rules to set as or prerequisites
     */
    public void setOrRules(Set<Rule> orRules) {
        this.orRules = orRules;
    }

    /**
     * Compute a warning message for this rule from a given start time.
     *
     * @param startTime a start time
     * @return a warning message
     */
    public String getWarningMessage(long startTime) {
        List<Long> keys = errorMessages.keySet().stream().sorted().collect(Collectors.toList());
        if (keys.isEmpty()) {
            return "No warning messages defined!";
        }
        long elapsedTime = Instant.now().getEpochSecond() - startTime;
        long hook = Collections.min(keys);
        for (long key : keys) {
            if (elapsedTime <= key) {
                return errorMessages.get(hook);
            } else {
                hook = key;
            }
        }
        /* elapsed time was higher than all keys in list */
        return errorMessages.get(Collections.max(keys));
    }

    /**
     * Get the groups which have to be notified now with the given start time.
     * The escalation maps elapsed time to the set of group names which should be notified.
     * If the elapsed time is on the boundaries of the map, the outermost element is returned.
     *
     * @param startTime the time at which the rule was first violated
     * @return the set of group names which should be notified now when the rule was violated at the given startTime
     */
    public Set<String> getEscalationLevelViewGroups(long startTime) {
        List<Long> keys = escalation.keySet().stream().sorted().collect(Collectors.toList());
        if (keys.isEmpty()) {
            return new HashSet<>();
        }
        long elapsedTime = Instant.now().getEpochSecond() - startTime;
        long hook = Collections.min(keys);
        for (long key : keys) {
            if (elapsedTime <= key) {
                return escalation.get(hook);
            } else {
                hook = key;
            }
        }
        /* elapsed time was higher than all keys in list */
        return escalation.get(Collections.max(keys));
    }

    /**
     * The viewpermissions are the union of all defined escalation permissions and the explicit view permissions.
     * @return the set of groups the members of which should be allowed to view this rule
     */
    public Set<String> getViewPermissions() {
        return viewPermissions;
    }

    /**
     * Get the message which should be displayed if this rule is ok.
     * @return the ok message
     */
    public String getOkMessage() {
        return okMessage;
    }

    /**
     * A rule can be set invisible to more elegantly define rules which depend on others.
     *
     * @return whether this rule should directly be shown in the frontend.
     */
    public boolean isVisible() {
        /* Yoda logic: necessary, because otherwise a field which is not set in the rule definitions
         * will be set to false. This way, invisible is set to false.
         */
        return !invisible;
    }

    /**
     * Getter for logs which were set related so that the information can be used from the frontend.
     *
     * @return the related logs
     */
    public Set<String> getRelatedLogs() {
        return relatedLogNames;
    }

    /**
     * Specific evaluation of a concrete rule on a model.
     *
     * @param model the model to use information from
     * @return the rule state, containing violated and passed sensors
     */
    protected abstract RuleState specificEval(FHEMModel model);

    /**
     * Call evaluation with an initially empty set to track the visited rules.
     *
     * @param model the model to evaluate on
     * @return a rule state with the violated and passed rules
     */
    public RuleState eval(FHEMModel model) {
        return eval(model, new HashSet<>());
    }

    /**
     * Evaluate a rule.
     * If the evaluation already happened, the result will be cached.
     * The evaluation keeps track of visited places to avoid getting stuck in an infinite loop (cycle).
     * Then, the preconditions are evaluated (required true/false rules).
     * Lastly, the specific evaluation of the concrete underlying rule is called.
     * This is a recursive method with many subcalls, but every call that results in a successful evaluation
     * will cache the result. As a consequence, only n * m rules are evaluated, with n being the number of rules,
     * and m being the amount of sensors.
     *
     * @param model        the model to acquire information from
     * @param visitedRules the currently visited rules (initialised to empty set by the
     * {@link webserver.ruleCheck.rules.Rule#eval(webserver.fhemParser.fhemModel.FHEMModel) helper method})
     * @return the state of this rule, consisting of passed and violated sensors
     */
    private RuleState eval(FHEMModel model, Set<Rule> visitedRules) {
        if (!visitedRules.add(this)) {
            System.err.println("There was a cyclic rule dependency involving " + visitedRules.size()
                    + " rules! Breaking the cycle by assuming this rule is violated.");
            System.err.println("This will invalidate all rules in the cycle.");
            Set<String> allSensorNames = new HashSet<>(sensorNames);
            for (Rule visited : visitedRules) {
                allSensorNames.addAll(visited.sensorNames);
            }
            /* Not setting to evaluated because another eval might still pass by */
            return new RuleState(this, new HashSet<>(), model.getSensorsByCollection(sensorNames));
        }

        /* Prevent repeated calls to eval (which might happen due to interdependencies) to reevaluate a known result */
        if (isEvaluated) {
            assert ruleState != null;
            return ruleState;
        }

        /* Handle preconditions (rules which are specified to be true or false in order for this rule to even apply */
        boolean andRulesOk = true;
        boolean orRulesOk = orRules.isEmpty();

        for (Rule andRule : andRules) {
            if (!andRule.eval(model, visitedRules).isOk()) {
                andRulesOk = false;
                break;
            }
        }

        for (Rule orRule : orRules) {
            if (orRule.eval(model, visitedRules).isOk()) {
                orRulesOk = true;
                break;
            }
        }

        /* Return early if not all preconditions are met. */
        if (!andRulesOk || !orRulesOk) {
            isEvaluated = true;
            /* Not all preconditions have been met. This rule is violated. */
            ruleState = new RuleState(this, new HashSet<>(), model.getSensorsByCollection(sensorNames));
            return ruleState;
        }
        return specificEval(model);
    }

    public Set<String> getAllViewGroups() {
        return viewPermissions;
    }

    /**
     * This rule can be marked as important in it's definition, which means
     * it should be shown in the frontend mainscreen.
     * @return true if this rule is important
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * Return the defined priority of this rule.
     * @return the defined priority
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Rule{"
                + "name='" + name + '\''
                + ", viewPermissions='" + viewPermissions + '\''
                + ", expression='" + expression + '\''
                + '}';
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        return name.equals(rule.name);
    }
}
