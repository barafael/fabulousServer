package webserver;

import webserver.fhemParser.FHEMParser;
import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.fhemParser.fhemModel.sensors.FHEMSensor;
import webserver.stateCheck.StateChecker;
import webserver.stateCheck.WARNINGLEVEL;
import webserver.stateCheck.parsing.RuleParam;
import webserver.stateCheck.parsing.RuleParamCollection;
import webserver.stateCheck.parsing.RuleType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Tests for the StateChecker. Some tests depend on json files in the root directory.
 * All tests which get a FHEM model depend on local copies of FHEM state files.
 * The directory $FHEMMOCKDIR (defined as a global shell environment variable) should contain
 * filelogs from fhem as well as a current copy of the output of FHEM's jsonList2 command.
 * This can be achieved by executing the {@code pull.sh} file from the root directory.
 * @author Rafael
 */
public class StateCheckerTest {
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
                escalation);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        RuleParamCollection col = new RuleParamCollection(ruleParam);
        String json = gson.toJson(col);
        System.out.println(json);
        assert isValidJSON(json);
    }

    @Test
    public void testStateCheckerEvaluate() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        StateChecker stateChecker = StateChecker.getInstance();
        stateChecker.evaluate(model, Optional.empty());
    }

    @Test
    public void testInvalidRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        StateChecker stateChecker = StateChecker.getInstance();
        stateChecker.evaluate(model, Optional.of("noRainNoDry.json"));

        Optional<FHEMSensor> sensor_opt = model.getSensorByName("HM_4F5DAA_Rain");
        assert sensor_opt.isPresent();
        FHEMSensor sensor = sensor_opt.get();

        assert sensor.getRuleInfo().stream().filter(s -> s.getName().equals("NeverTrue")).count() == 1;
    }

    @Test
    public void testDuplicateRule() {
        Optional<FHEMModel> model_opt = FHEMParser.getInstance().getFHEMModel();
        assert model_opt.isPresent();
        FHEMModel model = model_opt.get();

        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get("duplicateRule.json")));
        } catch (IOException e) {
            assert false;
            return;
        }
        assert RuleParamCollection.fromJson(json).toRules().size() == 1;
    }
}
