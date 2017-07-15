package webserver.stateCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.parsing.RuleParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This rule allows to call a public predicate method in
 * {@link webserver.fhemParser.fhemModel.sensors.FHEMSensor FHEMSensor},
 * which will be evaluated and its result used as evaluation for the concrete rule.
 *
 * @author Rafael on 07.07.17.
 */
public final class SensorPredicate extends Rule {
    public SensorPredicate(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState specificEval(FHEMModel model) {
        Set<FHEMSensor> okSensors = new HashSet<>();
        Set<FHEMSensor> violatedSensors = new HashSet<>();

        String[] tokens = expression.split(" ");
        if (tokens.length <= 1 || !tokens[0].equals("Sensor")) {
            System.err.println(
                    "Expression for sensor predicate rule must have at least a function name after the 'Sensor' prefix");
            /* Return false to draw attention to formulation error (?) */
            violatedSensors.addAll(model.getSensorsByCollection(sensorNames));
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), violatedSensors);
            return ruleState;
        }

        String methodName = tokens[1];
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

            /* Manual type checks */
            /* Return type */
            if (!method.getReturnType().getName().equals("boolean")) {
                System.err.println("The method " + methodName + " you tried to call on sensor " + sensorName + " "
                        + "does not return a boolean!");
                violatedSensors.add(sensor);
                continue;
            }

            /* Parameter type */
            Class<?>[] actualTypes = method.getParameterTypes();
            /* Cannot check for type of List due to type erasure,
            but can at least check that the type is List<> and that there is exactly one parameter.
            */
            if (actualTypes.length != 1 || !actualTypes[0].getName().equals("java.util.List")) {
                violatedSensors.add(sensor);
                System.err.println("The parameter of " + methodName + " must be exactly one List<String>.");
                continue;
            }

            /* Invoke method with arguments and get back the result */
            try {
                ruleOK = Boolean.parseBoolean(method.invoke(sensor, arguments).toString());
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
                System.err.println("The function " + methodName + " caused a " + e.getTargetException()
                        + " on the sensor " + sensorName);
                violatedSensors.add(sensor);
                e.printStackTrace();
                continue;
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
