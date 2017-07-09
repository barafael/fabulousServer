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
 *
 * TODO: implement AND and OR rules?
 * TODO: implement 'invisible' attribute, which makes a rule invisible for the app but can still be used from other rules?
 */

public abstract class Rule {
    String name;
    String permission;
    /* sensor names and not aliases, to guarantee uniqueness */
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

    public abstract RuleState eval(FHEMModel model);

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
