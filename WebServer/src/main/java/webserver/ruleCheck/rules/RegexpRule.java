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
public final class RegexpRule extends Rule {
    /**
     * Construct a regexp rule.
     *
     * @param ruleParam the parameters of the general predicate
     */
    public RegexpRule(RuleParam ruleParam) {
        super(ruleParam);
    }

    /**
     * Specific evaluation of a regexp rule on a model.
     *
     * @param model the model to use information from
     * @return the rule state, containing violated and passed sensors
     */
    @Override
    public RuleState specificEval(FHEMModel model) {
        /*
        Result accumulators
        */
        Set<FHEMSensor> okSensors = new HashSet<>();
        Set<FHEMSensor> violatedSensors = new HashSet<>();

        /*
        Input validation
        */
        String[] tokens = expression.split(" ");
        if (tokens.length != 3) {
            System.err.println("Expression for RegexRule must have three elements separated by a space.");
            /* Return false to draw attention to formulation error (?) */
            violatedSensors.addAll(model.getSensorsByCollection(sensorNames));
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), violatedSensors);
            return ruleState;
        }

        /*
        Espression parsing
        */
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

            Optional<String> value_opt = sensor.getValueOfField(field);
            if (!value_opt.isPresent()) {
                System.err.println("The field '" + field
                        + "' specified in this rule: " + name
                        + " is not implemented or does not exist in this sensor: " + sensor.getName());
                violatedSensors.add(sensor);
                continue;
            }

            String value = value_opt.get();

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
                    System.err.println("This operator " + operator + " is unimplemented for regexp rule!");
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
