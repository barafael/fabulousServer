package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.stateCheck.WARNINGLEVEL;
import WebServer.stateCheck.rules.parsing.RuleParam;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This abstract class contains the attributes of a rule, most notably the eval() method.
 *
 * @author Rafael
 */

public abstract class Rule {
    String name;
    String permission;
    Set<String> sensorNames;
    String expression;
    String okMessage;
    Map<WARNINGLEVEL, String> errorMessages;
    Map<Long, WARNINGLEVEL> escalation = new TreeMap<>();

    boolean isEvaluated = false;
    RuleState ruleState = null;
    Set<Rule> requiredTrueRules;
    Set<Rule> requiredFalseRules;

    public Rule(RuleParam ruleParam) {
        name = ruleParam.getName();
        permission = ruleParam.getPermissionField();
        sensorNames = ruleParam.getSensorNames();
        expression = ruleParam.getExpression();
        okMessage = ruleParam.getOkMessage();
        errorMessages = ruleParam.getErrorMessages();
        escalation = ruleParam.getEscalation();
    }

    public abstract RuleState realEval(FHEMModel model);

    public RuleState eval(FHEMModel model) {
        /* TODO pull specific, repeated logic in method that calls concrete eval() */
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

    @Override
    public String toString() {
        return "Rule{" +
                "name='" + name + '\'' +
                ", permission='" + permission + '\'' +
                ", expression='" + expression + '\'' +
                '}';
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
}
