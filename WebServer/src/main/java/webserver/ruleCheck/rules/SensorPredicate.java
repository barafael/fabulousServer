package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.parsing.RuleParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This rule allows to call a public predicate method in
 * {@link webserver.fhemParser.fhemModel.sensors.FHEMSensor FHEMSensor},
 * which will be evaluated and its result used as evaluation for the concrete rule.
 *
 * @author Rafael on 07.07.17.
 */
public final class SensorPredicate extends Rule {
    /**
     * Construct a sensor predicate.
     * @param ruleParam the parameters of the general predicate
     */
    public SensorPredicate(RuleParam ruleParam) {
        super(ruleParam);
    }

    /**
     * Specific evaluation of a sensor predicate rule on a model.
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
        if (tokens.length <= 1 || !tokens[0].equals("Sensor")) {
            System.err.println(
                    "Expression for sensor predicate rule must have at least "
                            + "a function name after the 'Sensor' prefix");
            /* Return false to draw attention to formulation error (?) */
            violatedSensors.addAll(model.getSensorsByCollection(sensorNames));
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), violatedSensors);
            return ruleState;
        }

        /*
        Function and argument parsing
         */
        String methodName = tokens[1];

        boolean negate = false;
        if (methodName.startsWith("not")) {
            negate = true;
            methodName = methodName.substring(3);
        }

        List<String> arguments = new ArrayList<>();
        if (tokens.length > 2) {
            arguments.addAll(Arrays.asList(tokens).subList(2, tokens.length));
        }
        for (String sensorName : sensorNames) {
            Optional<FHEMSensor> sensor_opt = model.getSensorByName(sensorName);
            if (!sensor_opt.isPresent()) {
                System.err.println("The sensor name " + sensorName + " was not found in the FHEM model!");
                continue;
            }

            FHEMSensor sensor = sensor_opt.get();

            boolean ruleOK;

            /* Get method */
            Method method;
            try {
                method = sensor.getClass().getMethod(methodName, List.class);
            } catch (SecurityException e) {
                System.err.println(sensor + ": There was a security exception when calling "
                        + methodName + "! This is unexpected.");
                violatedSensors.add(sensor);
                e.printStackTrace();
                continue;
            } catch (NoSuchMethodException e) {
                System.err.println(sensor + ": The method " + methodName + " was not found in the class FHEMSensor.");
                violatedSensors.add(sensor);
                e.printStackTrace();
                continue;
            }

            /* Manual type checks
             * If getMethod() succeeded, the parameter types matched
             */
            if (!method.getReturnType().getName().equals("boolean")) {
                System.err.println("The method " + methodName + " you tried to call on sensor " + sensorName + " "
                        + "does not return a boolean!");
                violatedSensors.add(sensor);
                continue;
            }

            /* Invoke method with arguments and get back the result */
            try {
                ruleOK = Boolean.parseBoolean(method.invoke(sensor, arguments).toString());
                if (negate) {
                    ruleOK = !ruleOK;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("There was an illegal argument exception when calling "
                        + methodName + " on " + sensorName);
                violatedSensors.add(sensor);
                e.printStackTrace();
                continue;
            } catch (IllegalAccessException e) {
                System.err.println("There was an illegal access: called " + methodName + " on " + sensorName);
                violatedSensors.add(sensor);
                e.printStackTrace();
                continue;
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof ClassCastException) {
                    System.err.println("You might have defined a predicate with the wrong parameter types! "
                            + "Parameter type must be exactly one List<String>.");
                } else {
                    System.err.println("The function " + methodName + " caused a " + e.getTargetException()
                            + " on the sensor " + sensorName);
                }
                e.printStackTrace();
                violatedSensors.add(sensor);
                continue;
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
