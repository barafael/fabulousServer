package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.parsing.RuleParam;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A specific rule implementation for a rule to match on fields of sensors via regex or literal terms.
 *
 * @author Rafael on 07.07.17.
 */
public class RegexpRule extends Rule {

    public RegexpRule(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState specificEval(FHEMModel model) {
        Set<FHEMSensor> okSensors = new HashSet<>();
        Set<FHEMSensor> violatedSensors = new HashSet<>();

        String[] tokens = expression.split(" ");
        if (tokens.length != 3) {
            System.err.println("Expression for RegexRule must have three elements separated by a space.");
            /* Return false to draw attention to formulation error (?) */
            violatedSensors.addAll(model.getSensorsByCollection(sensorNames));
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), violatedSensors);
            return ruleState;
        }

        String field = tokens[0];
        String operator = tokens[1];
        String literal = tokens[2];

        for (String sensorName : sensorNames) {
            Optional<FHEMSensor> sensor_opt = model.getSensorByName(sensorName);
            if (!sensor_opt.isPresent()) {
                System.err.println("The sensor name " + sensorName + " was not found in the FHEM model!");
                continue;
            }

            FHEMSensor sensor = sensor_opt.get();

            boolean ruleOK;

            Optional<String> concField_opt = sensor.getValueOfField(field);
            if (!concField_opt.isPresent()) {
                System.err.println("The field '" + field
                        + "' specified in this rule: " + name
                        + " is not implemented or does not exist in this sensor: " + sensor.getName());
                violatedSensors.add(sensor);
                continue;
            }

            String value = concField_opt.get();

            switch (operator) {
                case "startsWith":
                    ruleOK = value.startsWith(literal);
                    break;
                case "endsWith":
                    ruleOK = value.endsWith(literal);
                    break;
                case "contains":
                    ruleOK = value.contains(literal);
                    break;
                case "equals":
                    ruleOK = value.equals(literal);
                    break;
                case "matches":
                    ruleOK = value.matches(literal);
                    break;
                case "notcontains":
                    ruleOK = !value.contains(literal);
                    break;
                case "notequals":
                    ruleOK = !value.equals(literal);
                    break;
                case "notmatches":
                    ruleOK = !value.matches(literal);
                    break;
                default:
                    System.err.println("This operator " + operator + " is unimplemented for REGEXP rule!");
                    ruleOK = false;
            }

            if (ruleOK) {
                okSensors.add(sensor);
            } else {
                violatedSensors.add(sensor);
            }
        }
        isEvaluated = true;
        boolean state = violatedSensors.isEmpty() && !okSensors.isEmpty();
        ruleState = new RuleState(state, okSensors, violatedSensors);
        return ruleState;
    }
}
