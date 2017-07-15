package webserver.stateCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.PredicateCollection;
import webserver.stateCheck.parsing.RuleParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This rule allows to call a public predicate method in
 * {@link webserver.stateCheck.PredicateCollection PredicateCollection},
 * which will be evaluated and its result used as evaluation for the concrete rule.
 *
 * @author Rafael on 07.07.17.
 */
public final class GeneralPredicate extends Rule {
    public GeneralPredicate(RuleParam ruleParam) {
        super(ruleParam);
    }

    @Override
    public RuleState specificEval(FHEMModel model) {
        String[] tokens = expression.split(" ");
        if (tokens.length <= 1 || !tokens[0].equals("Predicate")) {
            System.err.println(
                    "Expression for general predicate rule must have at least a function name after the 'Predicate' prefix");
            /* Return false to draw attention to formulation error (?) */
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        }

        String methodName = tokens[1];
        List<String> arguments = new ArrayList<>();
        if (tokens.length > 2) {
            arguments.addAll(Arrays.asList(tokens).subList(2, tokens.length));
        }

        if (!sensorNames.isEmpty()) {
            System.err.println("Found a general predicate rule which depends on sensors. The sensors will be ignored. "
                    + "Use a sensor predicate rule if you want the rule to depend on a sensor's state.");
        }

        boolean ruleOK;

        PredicateCollection predicateCollection = new PredicateCollection();
        /* Get method */
        Method method;
        try {
            method = predicateCollection.getClass().getMethod(methodName, List.class);
        } catch (SecurityException e) {
            System.err.println("There was a security exception when accessing " + methodName
                    + " from the predicate collection! This is unexpected.");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;

        } catch (NoSuchMethodException e) {
            System.err.println(predicateCollection + ": The method "
                    + methodName + " was not found in the predicate collection.");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        }

        /* Manual type checks */
        /* Return type */
        if (!method.getReturnType().getName().equals("boolean")) {
            System.err.println("The method " + methodName + " you tried to call in the predicate collection "
                    + "does not return a boolean!");
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        }

        /* Parameter type */
        Class<?>[] actualTypes = method.getParameterTypes();
        /* Cannot check for type of List due to type erasure,
        but can at least check that the type is List<> and that there is exactly one parameter.
        */
        if (actualTypes.length != 1 || !actualTypes[0].getName().equals("java.util.List")) {
            System.err.println("The parameter of " + methodName + " must be exactly one List<String>.");
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        }

        /* Invoke method with arguments and get back the result */
        try {
            ruleOK = Boolean.parseBoolean(method.invoke(predicateCollection, arguments).toString());
        } catch (IllegalArgumentException e) {
            System.err.println("There was an illegal argument exception when calling "
                    + methodName + " on the predicate collection.");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        } catch (IllegalAccessException e) {
            System.err.println("There was an illegal access: called " + methodName + " on the predicate collection");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ClassCastException) {
                System.err.println("You might have defined a predicate with the wrong parameter types! " +
                        "Parameter type must be exactly one List<String>.");
            } else {
                System.err.println("The function " + methodName + " caused a " + e.getTargetException()
                        + " on the predicate collection.");
            }
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(false, new HashSet<>(), new HashSet<>());
            return ruleState;
        }
        isEvaluated = true;
        ruleState = new RuleState(ruleOK, new HashSet<>(), new HashSet<>());
        return ruleState;
    }
}
