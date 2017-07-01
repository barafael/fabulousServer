package WebServer.stateCheck;

import WebServer.FHEMParser.fhemModel.FHEMModel;
import WebServer.stateCheck.rules.Rule;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Rafael
 */

public class StateChecker {
    private final Set<Rule> rules = new HashSet<>();

    public boolean registerRule(Rule r) {
        return rules.add(r);
    }

    public boolean evaluate(FHEMModel model) {
        State state = new State();
        return rules.stream().map(r -> r.eval(model, state)).anyMatch(b -> !b);
    }
}
