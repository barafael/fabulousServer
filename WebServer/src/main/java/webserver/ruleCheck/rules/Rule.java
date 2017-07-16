package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.WARNINGLEVEL;
import webserver.ruleCheck.parsing.RuleParam;

import java.time.Instant;
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
 *         TODO: implement 'invisible' attribute, which makes a rule invisible for the app but can still be used from other rules?
 */
public abstract class Rule {
    /**
     * A set of names for which this rule applies.
     * Sensor names should be used (and not aliases) to guarantee uniqueness
     */
    final Set<String> sensorNames;
    /**
     * The expression which should be evaluated for each sensor, like 'State matches d*y'.
     */
    final String expression;
    /**
     * The name of the rule.
     */
    final String name;
    /**
     * The permissions for this rule to be shown.
     */
    private final String permission;
    @SuppressWarnings("FieldCanBeLocal")
    private final String okMessage;
    private final Map<WARNINGLEVEL, String> errorMessages;
    /**
     * The state of this rule, consisting of a state holder boolean, and sets of sensors which are ok/violated.
     */
    RuleState ruleState;
    /**
     * Whether this rule has already been evaluated.
     */
    boolean isEvaluated = false;
    private Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();
    private Set<Rule> requiredTrueRules;
    private Set<Rule> requiredFalseRules;
    private boolean isVisibleInApp = true;

    Rule(RuleParam ruleParam) {
        name = ruleParam.getName();
        permission = ruleParam.getPermissionField();
        sensorNames = ruleParam.getSensorNames();
        expression = ruleParam.getExpression();
        okMessage = ruleParam.getOkMessage();
        errorMessages = ruleParam.getErrorMessages();
        escalation = ruleParam.getEscalation();
        isVisibleInApp = ruleParam.getVisible();
    }

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

    private RuleState eval(FHEMModel model, Set<Rule> visited) {
        if (!visited.add(this)) {
            System.err.println("There was a cyclic rule dependency involving " + visited.size() + " rules! Breaking the cycle by assuming this rule is violated.");
            System.err.println("This will invalidate all rules in the cycle.");
            /* Not setting to evaluated because another eval might still pass by */
            return new RuleState(this, new HashSet<>(), model.getSensorsByCollection(sensorNames));
        }

        /* Prevent repeated calls to eval (which might happen due to interdependencies) to reevaluate a known result */
        if (isEvaluated) {
            /* TODO: When is this cleared? */
            assert ruleState != null;
            return ruleState;
        }

        /* Handle preconditions (rules which are specified to be true or false in order for this rule to even apply */
        boolean trueRulesOK = true;
        boolean falseRulesOK = true;

        for (Rule trueRule : requiredTrueRules) {
            if (!trueRule.eval(model, visited).isOk()) {
                trueRulesOK = false;
                break;
            }
        }

        for (Rule falseRule : requiredFalseRules) {
            if (falseRule.eval(model, visited).isOk()) {
                falseRulesOK = false;
                break;
            }
        }

        /* Return early if not all preconditions are met. */
        if (!trueRulesOK || !falseRulesOK) {
            isEvaluated = true;
            /* Not all preconditions have been met. This rule is violated. */
            ruleState = new RuleState(this, new HashSet<>(), model.getSensorsByCollection(sensorNames));
            return ruleState;
        }
        return specificEval(model);
    }

    public String getName() {
        return name;
    }

    public void setRequiredTrue(Set<Rule> requiredTrue) {
        this.requiredTrueRules = requiredTrue;
    }

    public String getWarningMessage(long startTime) {
        long elapsedTime = Instant.now().getEpochSecond() - startTime;
        List<Long> keys = escalation.keySet().stream().sorted().collect(Collectors.toList());
        for (long key : keys) {
            if (elapsedTime <= key) {
                return errorMessages.get(escalation.get(key));
            }
        }
        /* elapsed time was higher than all keys in list */
        return errorMessages.get(WARNINGLEVEL.DISASTER);
    }

    public void setRequiredFalse(Set<Rule> requiredFalse) {
        this.requiredFalseRules = requiredFalse;
    }

    public String getPermissionField() {
        return permission;
    }

    public String getOkMessage() {
        return okMessage;
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

        /* Due to technical reasons discussed in the rule checker documentation,
        this ugly hack works but should not be trusted.
         */
        /* TODO: Find a better way to do this or don't do it and update the documentation
        boolean equals = name.equals(rule.name);
        if (equals) {
            System.err.println("Duplicate rule detected! " + name);
        }
        return equals;
        */
        return name.equals(rule.name);
    }

    @Override
    public String toString() {
        return "Rule{"
                + "name='" + name + '\''
                + ", permission='" + permission + '\''
                + ", expression='" + expression + '\''
                + '}';
    }
}
