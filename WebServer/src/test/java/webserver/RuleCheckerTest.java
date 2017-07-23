package webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import webserver.fhemParser.FHEMParser;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.ruleCheck.PredicateCollection;
import webserver.ruleCheck.RuleChecker;
import webserver.ruleCheck.parsing.RuleParam;
import webserver.ruleCheck.parsing.RuleParamCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tests for the RuleChecker. Some tests depend on json files in the root directory.
 * <p>
 * All tests which get a FHEM model depend on a local copy of jsonList2.
 * The pullData function (annotated with @BeforeClass) pulls the mocking data if it is not present.
 *
 * @author Rafael
 */
public class RuleCheckerTest {
    /**
     * Conditionally pulls the mocking data to the local machine (if it is not yet present).
     * This can take a moment, depending on load and traffic.
     *
     * @throws IOException if there was an I/O error during command execution.
     */
    @BeforeClass
    public static void pullData() throws IOException {
        if (Files.exists(Paths.get("/tmp/fhemlog/"))) {
            return;
        }
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "./pull.sh"});
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        /* To wait until done */
        stdin.readLine();
        stdin.close();
    }

    /**
     * Reset the FHEM state.
     */
    @Before
    public void cleanState() {
        RuleChecker.getInstance().clear();
    }

    /**
     * Conditionally pulls jsonList2 to local machine
     * This can take a moment, depending on load and traffic.
     * It is like pull.sh, except for jsonList2.json only.
     *
     * @throws IOException if there was an I/O error during command execution.
     */
    private static void jullData() throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "./jull.sh"});
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        /* To wait until done */
        stdin.readLine();
        stdin.close();
    }

    /**
     * Helper function which tests if a json string can be deserialized without throwing an exception
     * (which would mean incorrect json format).
     *
     * @param json A string which should be tested
     * @return whether the input was valid json
     */
    private static boolean isValidJSON(String json) {
        Gson gson = new Gson();
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    /**
     * Construct a rule directly from parameters.
     * This is also useful to check out how Gson deserializes a ruleparam.
     * After adding a new feature to the parameters, it can be used to
     * find out how to correctly write the rule in the first place.
     */
    @Test
    public void testConstructRuleParam() {
        Set<String> sensorlist = new HashSet<>(Arrays.asList("sensor1", "sensor2", "sensor3"));
        Set<String> reqTrue = new HashSet<>(Arrays.asList("testRule", "anotherRule", "yetAnotherRule"));
        Map<Long, String> errorMessages = new HashMap<>();
        errorMessages.put(10L, "There is a 10 second problem");
        errorMessages.put(60L, "wrong for a minute");
        errorMessages.put(600L, "It is getting important");
        errorMessages.put(1000L, "FIX IT!!!");
        RuleParam ruleParam = new RuleParam("Fenster1",
                sensorlist,
                new HashSet<>(Collections.singletonList("permission1")),
                "Readings contains dry",
                reqTrue,
                "All Good",
                errorMessages,
                new HashMap<>(),
                true,
                Collections.emptySet(),
                12
        );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        RuleParamCollection col = new RuleParamCollection(ruleParam);
        String json = gson.toJson(col);
        assert isValidJSON(json);
    }

    /**
     * The default rules are in the top-lvl dir 'rules.json'.
     * Evaluating these is inconclusive because the content changes over time.
     *
     * @throws IOException if I/O goes wrong
     */
    @Test
    public void testStateCheckerEvaluateDefaultRules() throws IOException {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();
    }

    @Test
    public void testStateCheckerEvaluateWindowOpen() throws IOException {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/windowOpen.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        RuleChecker ruleChecker = RuleChecker.getInstance();

        boolean interactive_testing = false;

        //noinspection ConstantConditions
        while (interactive_testing) {
            jullData();
            model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/windowOpen.json");
            assert model_opt.isPresent();
        }
    }

    /**
     * This test defines a regexp rule which has a non-satisfiable expression.
     * The model should contain violated ruleinfo for a specific sensor.
     */
    @Test
    public void testEvaluateImpossibleRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/impossibleRainRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        FHEMSensor rainSensor = model.getSensorByName("HM_4F5DAA_Rain").get();
        assert rainSensor.getViolatedRules().stream().findAny().isPresent();
        assert rainSensor.getViolatedRules().stream().findAny().get()
                .getName().equals("impossibleRainRule");
    }

    /**
     * Evaluates a rule which should always be true for this sensor.
     * The regexp rule matches dry|rain on the state of the sensor.
     */
    @Test
    public void testEvaluateAlwaysTrueRuleWithOkInfo() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/alwaysTrue.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getViolatedRules().size() == 0;
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getPassedRules().size() == 1;
    }

    /**
     * Evaluates a rule which should be incorrect JSON.
     * Gson incorrectly parses the input anyway, but it is filtered out later to avoid NullPointerExceptions.
     */
    @Test
    public void testEvaluateInvalidInput() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/incorrectComma.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getViolatedRules().size() == 1;

        assert model.getSensorByName("HM_4F5DAA_Rain").get().getViolatedRules().stream()
                .filter(s -> s.getName().equals("incorrectJSONRule")).count() == 1;
    }

    /**
     * Evaluate more than one rule per sensor.
     * door sensor is tested, (open or closed) and battery ok
     */
    @Test
    public void testEvaluateMultipleRulesPerSensor() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/multipleRulesPerSensor.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_56A86F").isPresent();
        assert model.getSensorByName("HM_56A86F").get().getViolatedRules().size() == 2;
    }

    /**
     * Handle a cyclic dependency in the input.
     */
    @Test
    public void testCyclicRules() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/cyclicRuleDependencies.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        Optional<FHEMSensor> sensor_opt = model.getSensorByName("HM_4F5DAA_Rain");
        assert sensor_opt.isPresent();

        FHEMSensor sensor = sensor_opt.get();
        assert sensor.getViolatedRules().size() == 4;
    }

    /**
     * Evaluates a rule which will never be true.
     */
    @Test
    public void testNeverTrueRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/noRainNoDry.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        Optional<FHEMSensor> sensor_opt = model.getSensorByName("HM_4F5DAA_Rain");
        assert sensor_opt.isPresent();
        FHEMSensor sensor = sensor_opt.get();

        assert sensor.getViolatedRules().size() == 1;
        assert sensor.getViolatedRules().stream().filter(s -> s.getName().equals("NeverTrue")).count() == 1;
    }

    /**
     * Test direct construction of parameters from a json string.
     * The input file contains a duplicate rule, which should be filtered out.
     */
    @Test
    public void testParamConstruction() {
        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get(
                    "jsonRules/duplicateRule.json")));
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
            return;
        }
        assert RuleParamCollection.fromJson(json).toRules().size() == 1;
    }

    /**
     * Test ignoring of duplicate rules, without direct construction.
     * The input file contains a duplicate rule, which should be filtered out.
     */
    @Test
    public void testDuplicateRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/duplicateRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        Optional<FHEMSensor> sensor_opt = model.getSensorByName("HM_4F5DAA_Rain");
        assert sensor_opt.isPresent();
        FHEMSensor sensor = sensor_opt.get();

        assert sensor.getViolatedRules().size() == 1;
        assert sensor.getViolatedRules().stream().filter(s -> s.getName().equals("Duplicate")).count() == 1;
    }

    /**
     * Evaluate a rule which depends on multiple other rules.
     */
    @Test
    public void testRuleDependencies() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/ruleDependencies.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_4F5DAA_Rain").get();
        assert sensor.getViolatedRules().size() == 3;
    }

    /**
     * A rule which depends on it's own evaluation does not make sense and will lead to infinite recursion unless
     * filtered out.
     * It is important to catch because it might well happen as a careless and easy-to-make copy-paste error.
     */
    @Test
    public void testSelfCycleRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/selfCycle.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_4F5DAA_Rain").get();
        assert sensor.getViolatedRules().size() == 1;
    }

    /**
     * A rule for which permissions are given should appear in the output.
     */
    @Test
    public void testRulePermissionsAllowed() {
        Optional<String> json_opt = FHEMParser.getInstance().getFHEMModelJSON(Arrays.asList(
                "alwaysTruePermission", "permission1", "S_Fenster"), "jsonRules/permissionRule.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        FHEMSensor sensor2 = model.getSensorByName("HM_4F5DAA_Rain").get();
        assert sensor2.getPassedRules().size() == 1;
    }

    /**
     * Insufficient permissions for a rule. Sensor should not appear in the output.
     */
    @Test
    public void testRulePermissionsDisallowed() {
        Optional<String> json_opt = FHEMParser.getInstance()
                .getFHEMModelJSON(Arrays.asList(
                        "insufficientPermission", "permission1", "S_Fenster"), "jsonRules/permissionRule.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert !model.getSensorByName("HM_4F5DAA_Rain").isPresent();
    }

    /**
     * A rule for which permissions are given should appear in the output.
     */
    @Test
    public void testRulePermissionsOnlyRule() {
        Optional<String> json_opt = FHEMParser.getInstance().getFHEMModelJSON(Arrays.asList(
                "permission1", "S_Regen", "S_Regenregel"), "jsonRules/complexPermissionsRule.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        FHEMSensor sensor2 = model.getSensorByName("HM_4F5DAA_Rain").get();
        assert sensor2.getPassedRules().size() == 1;
        assert sensor2.getViolatedRules().size() == 0;
    }

    /**
     * Sufficient permissions for the sensor and a filelog, but not for a rule.
     */
    @Test
    public void testRulePermissionsDisallowedWithSensor() {
        Optional<String> json_opt = FHEMParser.getInstance()
                .getFHEMModelJSON(Arrays.asList(
                        "insufficientPermission", "permission1", "S_Regen"), "jsonRules/permissionRule.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getPassedRules().isEmpty();
    }

    @Test
    public void testWindowOpenDuringRain() {
        Optional<String> json_opt = FHEMParser.getInstance()
                .getFHEMModelJSON(Arrays.asList(
                        "insufficientPermission", "permission1", "S_Regen"), "jsonRules/windowsOpenDuringRain.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getPassedRules().size() == 1;
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getViolatedRules().isEmpty();
    }

    /**
     * Evaluates a numeric rule which is false at night.
     * This test might not pass during dawn or dusk.
     */
    @Test
    public void testEvaluateNumericRuleWithOkInfo() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/darknessRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_520B89").get();
        PredicateCollection pc = new PredicateCollection();
        if (pc.sunIsUp(Collections.emptyList())) {
            assert sensor.getViolatedRules().size() == 1;
            assert sensor.getPassedRules().size() == 0;
        } else {
            assert sensor.getViolatedRules().size() == 0;
            assert sensor.getPassedRules().size() == 1;
        }
    }

    /**
     * Evaluates a numeric rule which is true if the air is ok.
     */
    @Test
    public void testEvaluateCO2Limit() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/CO2LimitRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("netatmo_D70_ee_50_02_b8_20").isPresent();
        FHEMSensor sensor = model.getSensorByName("netatmo_D70_ee_50_02_b8_20").get();
        assert sensor.getViolatedRules().size() == 0;
        assert sensor.getPassedRules().size() == 1;
    }

    /**
     * Evaluate a sensor predicate rule which will always be true.
     */
    @Test
    public void testSensorPredicate() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/sensorPredicateRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_520B89").get();

        assert sensor.getPassedRules().size() == 1;
    }

    /**
     * Evaluate a sensor predicate rule which will always be true.
     */
    @Test
    public void testNegatedSensorPredicate() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/negatedSensorPredicateRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_520B89").get();

        assert sensor.getViolatedRules().size() == 1;
    }

    /**
     * Evaluate a general predicate rule which will always be true.
     */
    @Test
    public void testGeneralPredicate() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/generalPredicateRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_520B89").get();

        /* For general predicate, all sensors are ignored! */
        assert sensor.getPassedRules().size() == 0;
        assert sensor.getViolatedRules().size() == 0;
    }

    /**
     * Evaluate a negated general predicate rule which will always be false.
     * The direct result has no influence and is marked as isShowInApp=false,
     * but another rule in the same file depends on the negated general predicate.
     */
    @Test
    public void testNegatedGeneralPredicate() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/negatedGeneralPredicateRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_4F5DAA_Rain").get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensorUnaffected = model.getSensorByName("HM_520B89").get();

        /* For general predicate, all sensors are ignored! */
        assert sensorUnaffected.getPassedRules().size() == 0;
        assert sensorUnaffected.getViolatedRules().size() == 0;

        assert sensor.getPassedRules().size() == 0;
        assert sensor.getViolatedRules().size() == 1;
    }

    /**
     * Evaluate a sensor rule with incorrect expression.
     */
    @Test
    public void testIncorrectSensorPredicate() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel(
                "jsonRules/incorrectSensorPredicate.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_520B89").get();

        assert sensor.getPassedRules().size() == 0;
        assert sensor.getViolatedRules().size() == 0;
    }

    /**
     * This test is useful as an entry point for debugging only, as
     * it's success depends literally on the position of the stars.
     */
    @Ignore("Time dependent success")
    @Test
    public void testSun() {
        PredicateCollection predicateCollection = new PredicateCollection();
        assert predicateCollection.sunIsUp(new ArrayList<>());
    }

    /**
     * This test is useful as an entry point for debugging only, as
     * it's success depends on the time of day.
     */
    @Ignore("Time dependent success")
    @Test
    public void testWorkHours() {
        PredicateCollection predicateCollection = new PredicateCollection();
        /* Result obviously depends on the current time... */
        assert predicateCollection.isWorkingHours(new ArrayList<>());
    }
}
