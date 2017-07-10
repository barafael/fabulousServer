package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.StateChecker;
import WebServer.stateCheck.rules.parsing.RuleParam;
import WebServer.stateCheck.rules.parsing.RuleParamCollection;
import WebServer.stateCheck.rules.parsing.RuleType;
import com.google.gson.Gson;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Rafael
 */

public class StateCheckerTest {
    @Test
    public void testConstructRuleParam() {
        Set<String> sensorlist = new HashSet<>(Arrays.asList("sensor1", "sensor2", "sensor3"));
        RuleParam ruleParam = new RuleParam("Fenster1", sensorlist, RuleType.REGEXP, "permission1", "STATE contains dry", new HashSet<>(), new HashSet<>(), "all ok", new HashMap<>(), new HashMap<>());
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
        stateChecker.evaluate(model);
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
}
