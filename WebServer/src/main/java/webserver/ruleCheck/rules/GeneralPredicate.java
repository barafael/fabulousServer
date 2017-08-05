package webserver.ruleCheck.rules;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.PredicateCollection;
import webserver.ruleCheck.parsing.RuleParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * This rule allows to call a public predicate method in
 * {@link webserver.ruleCheck.PredicateCollection PredicateCollection},
 * which will be evaluated and its result used as evaluation for the concrete rule.
 *
 * @author Rafael on 07.07.17.
 */
public final class GeneralPredicate extends Rule {
    /**
     * Construct a general predicate.
     *
     * @param ruleParam the parameters of the general predicate
     */
    public GeneralPredicate(RuleParam ruleParam) {
        super(ruleParam);
    }

    /**
     * Specific evaluation of a general predicate on a model.
     *
     * @param model the model to use information from
     * @return the rule state, containing violated and passed sensors
     */
    @Override
    public RuleState specificEval(FHEMModel model) {
        /*
        Input validation
        */
        String[] tokens = expression.split("\\s+");
        if (tokens.length <= 1 || !tokens[0].equals("Predicate")) {
            System.err.println(
                    "Expression for general predicate rule must have at least"
                            + " a function name after the 'Predicate' prefix");
            /* Return false to draw attention to formulation error (?) */
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
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

        /*
        Rule-specific checks
         */
        if (!sensorNames.isEmpty()) {
            System.err.println("Found a general predicate rule which depends on sensors. The sensors will be ignored. "
                    + "Use a sensor predicate rule if you want the rule to depend on a sensor's state.");
        }

        /* Return value */
        boolean ruleOK;

        /* Get method from predicate collection */
        PredicateCollection predicateCollection = new PredicateCollection();
        Method method;
        try {
            method = predicateCollection.getClass().getMethod(methodName, List.class);
        } catch (SecurityException e) {
            System.err.println("There was a security exception when accessing " + methodName
                    + " from the predicate collection! This is unexpected.");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
            return ruleState;
        } catch (NoSuchMethodException e) {
            System.err.println(predicateCollection + ": The method "
                    + methodName + " was not found in the predicate collection.");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
            return ruleState;
        }

        /* Manual type checks
         * If getMethod() succeeded, the parameter types matched */
        if (!method.getReturnType().getName().equals("boolean")) {
            System.err.println("The method " + methodName + " you tried to call in the predicate collection "
                    + "does not return a boolean!");
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
            return ruleState;
        }

        /* Invoke method with arguments and get back the result */
        try {
            ruleOK = Boolean.parseBoolean(method.invoke(predicateCollection, arguments).toString());
            if (negate) {
                ruleOK = !ruleOK;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("There was an illegal argument exception when calling "
                    + methodName + " on the predicate collection.");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
            return ruleState;
        } catch (IllegalAccessException e) {
            System.err.println("There was an illegal access: called " + methodName + " on the predicate collection");
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
            return ruleState;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ClassCastException) {
                System.err.println("You might have defined a predicate with the wrong parameter types! "
                        + "Parameter type must be exactly one List<String>.");
            } else {
                System.err.println("The function " + methodName + " caused a " + e.getTargetException()
                        + " on the predicate collection.");
            }
            e.printStackTrace();
            isEvaluated = true;
            ruleState = new RuleState(this, new HashSet<>(), new HashSet<>());
            return ruleState;
        }
        isEvaluated = true;
        ruleState = new RuleState(ruleOK, this);
        return ruleState;
    }
}
