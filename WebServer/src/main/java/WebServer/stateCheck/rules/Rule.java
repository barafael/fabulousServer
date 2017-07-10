package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.WARNINGLEVEL;
import WebServer.stateCheck.rules.parsing.RuleParam;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This abstract class contains the attributes of a rule in fhem, most notably the eval(model) method.
 *  @author Rafael
 *
 * TODO: implement AND and OR rules?
 * TODO: implement 'invisible' attribute, which makes a rule invisible for the app but can still be used from other rules?
 */

public abstract class Rule {
    String name;
    private String permission;
    /* sensor names and not aliases, to guarantee uniqueness */
    protected Set<String> sensorNames;
    protected String expression;
    private String okMessage;
    private Map<WARNINGLEVEL, String> errorMessages;
    private Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();

    boolean isEvaluated = false;
    RuleState ruleState;
    private Set<Rule> requiredTrueRules;
    private Set<Rule> requiredFalseRules;

    Rule(RuleParam ruleParam) {
        name = ruleParam.getName();
        permission = ruleParam.getPermissionField();
        sensorNames = ruleParam.getSensorNames();
        expression = ruleParam.getExpression();
        okMessage = ruleParam.getOkMessage();
        errorMessages = ruleParam.getErrorMessages();
        escalation = ruleParam.getEscalation();
    }

    protected abstract RuleState realEval(FHEMModel model);

    public RuleState eval(FHEMModel model) {
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
            if (!trueRule.eval(model).isOk()) {
                trueRulesOK = false;
                break;
            }
        }

        for (Rule falseRule : requiredFalseRules) {
            if (falseRule.eval(model).isOk()) {
                falseRulesOK = false;
                break;
            }
        }

        /* Return early if not all preconditions are met. */
        if (!trueRulesOK || !falseRulesOK) {
            isEvaluated = true;
            /* Not all preconditions have been met. This rule is violated. */
            ruleState = new RuleState(false, new HashSet<>(), model.getSensorsByCollection(sensorNames));
            return ruleState;
        }

        return realEval(model);
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

    @Override
    public String toString() {
        return "Rule{" +
                "name='" + name + '\'' +
                ", permission='" + permission + '\'' +
                ", expression='" + expression + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        return name.equals(rule.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
