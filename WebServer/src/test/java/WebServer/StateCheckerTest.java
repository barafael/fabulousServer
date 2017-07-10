package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.FHEMParser.fhemModel.sensors.FHEMSensor;
import WebServer.stateCheck.StateChecker;
import WebServer.stateCheck.WARNINGLEVEL;
import WebServer.stateCheck.rules.parsing.RuleParam;
import WebServer.stateCheck.rules.parsing.RuleParamCollection;
import WebServer.stateCheck.rules.parsing.RuleType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.util.*;

/**
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
        Map<WARNINGLEVEL, String> warnings = new HashMap<>();
        warnings.put(WARNINGLEVEL.NORMAL, "all good");
        warnings.put(WARNINGLEVEL.LOW, "something is wrong");
        warnings.put(WARNINGLEVEL.HIGH, "hey, fix it!");
        Map<Long, WARNINGLEVEL> escalation = new HashMap<>();
        escalation.put(100L, WARNINGLEVEL.LOW);
        RuleParam ruleParam = new RuleParam("Fenster1", sensorlist, RuleType.REGEXP, "permission1", "STATE contains dry", new HashSet<>(), new HashSet<>(), "all ok", warnings, escalation);
        Gson gson = new Gson();
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
    }
}
