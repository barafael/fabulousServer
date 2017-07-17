package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.parsing.RuleParam;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This rule allows to compare a sensors numeric field to a given value.
 *
 * @author Rafael on 07.07.17.
 */
public final class NumericRule extends Rule {
    public NumericRule(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState specificEval(FHEMModel model) {
        Set<FHEMSensor> okSensors = new HashSet<>();
        Set<FHEMSensor> violatedSensors = new HashSet<>();

        String[] tokens = expression.split(" ");
        if (tokens.length != 3) {
            System.err.println("Expression for numeric rule must have three elements separated by a space.");
            /* Return false to draw attention to formulation error (?) */
            violatedSensors.addAll(model.getSensorsByCollection(sensorNames));
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), violatedSensors);
            return ruleState;
        }

        String field = tokens[0];
        String operator = tokens[1];
        double numberLiteral = Double.parseDouble(tokens[2]);

        for (String sensorName : sensorNames) {
            Optional<FHEMSensor> sensor_opt = model.getSensorByName(sensorName);
            if (!sensor_opt.isPresent()) {
                System.err.println("The sensor name " + sensorName + " was not found in the FHEM model!");
                continue;
            }

            FHEMSensor sensor = sensor_opt.get();

            boolean ruleOK;

            Optional<String> value_opt = sensor.getValueOfField(field);
            if (!value_opt.isPresent()) {
                System.err.println("The field '" + field
                        + "' specified in this rule: " + name
                        + " is not implemented or does not exist in this sensor: " + sensor.getName());
                violatedSensors.add(sensor);
                continue;
            }

            String value = value_opt.get();
            Matcher matcher = Pattern.compile("(\\d+(.\\d+)?)").matcher(value);
            if (!matcher.find()) {
                System.err.println("No number found in sensor status '" + field + "'");
            }
            double val = Double.valueOf(matcher.group());

            switch (operator) {
                case "<":
                    ruleOK = val < numberLiteral;
                    break;
                case "<=":
                    ruleOK = val <= numberLiteral;
                    break;
                case "==":
                    ruleOK = val == numberLiteral;
                    break;
                case ">=":
                    ruleOK = val >= numberLiteral;
                    break;
                case ">":
                    ruleOK = val > numberLiteral;
                    break;
                case "!=":
                    ruleOK = val != numberLiteral;
                    break;
                default:
                    System.err.println("This operator " + operator + " is unimplemented for NUMERIC rule!");
                    ruleOK = false;
            }

            if (ruleOK) {
                okSensors.add(sensor);
            } else {
                violatedSensors.add(sensor);
            }
        }
        isEvaluated = true;
        ruleState = new RuleState(this, okSensors, violatedSensors);
        return ruleState;
    }
}
