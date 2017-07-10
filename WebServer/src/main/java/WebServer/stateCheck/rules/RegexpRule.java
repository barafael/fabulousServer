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
    public RuleState realEval(FHEMModel model) {
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
        isEvaluated = true;
        boolean state = violatedSensors.isEmpty() && !okSensors.isEmpty();
        ruleState = new RuleState(state, okSensors, violatedSensors);
        return ruleState;
    }
}
