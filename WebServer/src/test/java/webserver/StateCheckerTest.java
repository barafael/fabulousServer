package webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import webserver.fhemParser.FHEMParser;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.StateChecker;
import webserver.stateCheck.WARNINGLEVEL;
import webserver.stateCheck.parsing.RuleParam;
import webserver.stateCheck.parsing.RuleParamCollection;
import webserver.stateCheck.parsing.RuleType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tests for the StateChecker. Some tests depend on json files in the root directory.
 * <p>
 * All tests which get a FHEM model depend on a local copy of jsonList2.
 * The directory $FHEMMOCKDIR (defined as a global shell environment variable) should contain
 * filelogs from fhem as well as a current copy of the output of FHEM's jsonList2 command.
 * This can be achieved by executing the {@code pull.sh} file from the root directory.
 *
 * Alternatively, the pullData function (annotated with @BeforeClass) pulls the data if it is not present.
 *
 * @author Rafael
 */
public class StateCheckerTest {
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
        stdin.readLine();
        stdin.close();
    }

    @Test
    public void testConstructRuleParam() {
        Set<String> sensorlist = new HashSet<>(Arrays.asList("sensor1", "sensor2", "sensor3"));
        Set<String> reqTrue = new HashSet<>(Arrays.asList("testRule", "anotherRule", "yetAnotherRule"));
        Set<String> reqFalse = new HashSet<>(Arrays.asList("Rule1", "Rule2", "Rule3"));
        Map<WARNINGLEVEL, String> warnings = new HashMap<>();
        warnings.put(WARNINGLEVEL.NORMAL, "all good");
        warnings.put(WARNINGLEVEL.LOW, "something is wrong");
        warnings.put(WARNINGLEVEL.HIGH, "hey, fix it!");
        Map<Long, WARNINGLEVEL> escalation = new HashMap<>();
        escalation.put(100L, WARNINGLEVEL.LOW);
        RuleParam ruleParam = new RuleParam("Fenster1",
                sensorlist,
                RuleType.REGEXP,
                "permission1",
                "STATE contains dry",
                reqTrue,
                reqFalse,
                "all ok",
                warnings,
                escalation,
                true);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        RuleParamCollection col = new RuleParamCollection(ruleParam);
        String json = gson.toJson(col);
        assert isValidJSON(json);
    }

    /**
     * Helper function which tests if a json string can be deserialized without throwing an exception
     * (which would mean incorrect json format).
     *
     * @param json A string which should be tested
     *
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
     * The default rules are in the top-lvl dir 'rules.json'.
     * Evaluating these is inconclusive because the content changes over time.
     */
    @Test
    public void testStateCheckerEvaluateDefaultRules() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        StateChecker stateChecker = StateChecker.getInstance();
        stateChecker.evaluate(model);
    }

    /**
     * This test defines a regexp rule which has a non-satisfiable expression.
     * The model should contain violated ruleinfo for a specific sensor.
     */
    @Test
    public void testEvaluateImpossibleRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/impossibleRainRule.json");
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
     * The regex rule matches dry|rain on the state of the sensor.
     */
    @Test
    public void testEvaluateAlwaysTrueRuleWithOkInfo() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/alwaysTrue.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getViolatedRules().size() == 0;
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getPassedRules().size() == 1;
    }

    /**
     * Evaluates a rule which should be incorrect JSON which gson incorrectly ignores currently.
     */
    @Test
    public void testEvaluateInvalidInput() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/incorrectComma.json");
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
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/multipleRulesPerSensor.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_56A86F").isPresent();
        assert model.getSensorByName("HM_56A86F").get().getViolatedRules().size() == 2;
    }

    /**
     * Evaluates a rule which can never be true. The Regexp will not be fulfilled by this rule.
     */
    @Test
    public void testCyclicRules() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/cyclicRuleDependencies.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        Optional<FHEMSensor> sensor_opt = model.getSensorByName("HM_4F5DAA_Rain");
        assert sensor_opt.isPresent();

        FHEMSensor sensor = sensor_opt.get();
        assert sensor.getViolatedRules().size() == 4;
    }

    /**
     * Evaluates a rule which can never be true. The Regexp will not be fulfilled by this rule.
     */
    @Test
    public void testNeverTrueRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/noRainNoDry.json");
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
            json = new String(Files.readAllBytes(Paths.get("jsonRules/duplicateRule.json")));
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
            return;
        }
        assert RuleParamCollection.fromJson(json).toRules().size() == 1;
    }

    /**
     * Test filter of duplicate rules, without direct construction.
     * The input file contains a duplicate rule, which should be filtered out.
     */
    @Test
    public void testDuplicateRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/duplicateRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        Optional<FHEMSensor> sensor_opt = model.getSensorByName("HM_4F5DAA_Rain");
        assert sensor_opt.isPresent();
        FHEMSensor sensor = sensor_opt.get();

        assert sensor.getViolatedRules().size() == 1;
        assert sensor.getViolatedRules().stream().filter(s -> s.getName().equals("Duplicate")).count() == 1;
    }

    @Test
    public void testRuleDependencies() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/ruleDependencies.json");
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
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/selfCycle.json");
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
                .getFHEMModelJSON(Arrays.asList("insufficientPermission", "permission1", "S_Fenster"), "jsonRules/permissionRule.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert !model.getSensorByName("HM_4F5DAA_Rain").isPresent();
    }

    /**
     * Permissions for the sensor and a filelog, but not for a rule.
     */
    @Test
    public void testRulePermissionsDisallowedWithSensor() {
        Optional<String> json_opt = FHEMParser.getInstance()
                .getFHEMModelJSON(Arrays.asList("insufficientPermission", "permission1", "S_Regen"), "jsonRules/permissionRule.json");
        assert json_opt.isPresent();
        String json = json_opt.get();

        FHEMModel model = new Gson().fromJson(json, FHEMModel.class);

        assert model.getSensorByName("HM_4F5DAA_Rain").isPresent();
        assert model.getSensorByName("HM_4F5DAA_Rain").get().getPassedRules().isEmpty();
    }

    /**
     * Evaluates a rule which should always be true for this sensor.
     * The regex rule matches dry|rain on the state of the sensor.
     */
    @Test
    public void testEvaluateAlwaysTrueNumericRuleWithOkInfo() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/darknessRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("HM_520B89").isPresent();
        FHEMSensor sensor = model.getSensorByName("HM_520B89").get();
        assert sensor.getViolatedRules().size() == 1;
        assert sensor.getPassedRules().size() == 0;
    }

    /**
     * Evaluates a rule which should always be true for this sensor.
     * The regex rule matches dry|rain on the state of the sensor.
     */
    @Test
    public void testEvaluateCO2Limit() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel("jsonRules/CO2LimitRule.json");
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        assert model.getSensorByName("netatmo_D70_ee_50_02_b8_20").isPresent();
        FHEMSensor sensor = model.getSensorByName("netatmo_D70_ee_50_02_b8_20").get();
        assert sensor.getViolatedRules().size() == 0;
        assert sensor.getPassedRules().size() == 1;
    }
}
