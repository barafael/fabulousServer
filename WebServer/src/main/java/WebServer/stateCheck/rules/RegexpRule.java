package WebServer.stateCheck.rules;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.stateCheck.rules.parsing.RuleParam;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Rafael on 07.07.17.
 */
public class RegexpRule extends Rule {

    public RegexpRule(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState eval(FHEMModel model) {
        /* TODO pull specific, repeated logic in method that calls concrete eval() */
        /* TODO implement cycle detection */
        /* Prevent repeated calls to eval (which might happen due to interdependencies) to reevaluate a known result */
        if (isEvaluated) {
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
        if (!trueRulesOK || falseRulesOK) {
            isEvaluated = true;
            /* Not all preconditions have been met. This rule is violated. */
            ruleState = new RuleState(false, new HashSet<>(), model.getSensorsByCollection(sensorNames));
            return ruleState;
        }

        Set<FHEMSensor> okSensors = new HashSet<>();
        Set<FHEMSensor> violatedSensors = new HashSet<>();

        String[] tokens = expression.split(" ");
        if (!(tokens.length == 3)) {
            System.err.println("Expression for RegexRule must have three elements separated by a space.");
            /* Return false to draw attention to formulation error (?) */
            violatedSensors.addAll(model.getSensorsByCollection(sensorNames));
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), violatedSensors);
            return ruleState;
        }

        String field = tokens[0];
        String operator = tokens[1];
        String expr = tokens[2];

        for (String sensorname : sensorNames) {
            Optional<FHEMSensor> sensor_opt = model.getSensorByName(sensorname);
            if (!sensor_opt.isPresent()) {
                System.err.println("The sensor name " + sensorname + " was not found in the FHEM model!");
                continue;
            }

            boolean ruleOK;

            FHEMSensor sensor = sensor_opt.get();

            Optional<String> concField_opt = sensor.getValueOfField(field);
            if (!concField_opt.isPresent()) {
                System.err.println("The field " + field +
                        " specified in this rule: " + name +
                        " is not implemented or does not exist in this sensor: " + sensor.getName());
                violatedSensors.add(sensor);
                continue;
            }

            String value = concField_opt.get();
            switch (operator) {
                case "startsWith":
                    ruleOK = value.startsWith(expr);
                    break;
                case "endsWith":
                    ruleOK = value.startsWith(expr);
                    break;
                case "contains":
                    ruleOK = value.contains(expr);
                    break;
                case "equals":
                    ruleOK = value.equals(expr);
                    break;
                case "matches":
                    ruleOK = value.matches(expr);
                    break;
                case "notmatches":
                    ruleOK = !value.matches(expr);
                    break;
                default:
                    System.err.println("This operator " + operator + " is unimplemented!");
                    ruleOK = false;
            }

            if (ruleOK) {
                okSensors.add(sensor);
            } else {
                violatedSensors.add(sensor);
            }
        }
        ruleState = new RuleState(violatedSensors.isEmpty(), okSensors, violatedSensors);
        isEvaluated = true;
        return ruleState;
    }
}
