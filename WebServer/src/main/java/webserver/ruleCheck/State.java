package webserver.ruleCheck;

import webserver.fhemParser.fhemModel.FHEMModel;
import webserver.ruleCheck.rules.GeneralPredicate;
import webserver.ruleCheck.rules.Rule;
import webserver.ruleCheck.rules.RuleState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps a state which can be applied to a FHEM model.
 *
 * @author Rafael
 */
class State {
    /**
     * Map a sensor name to the rules it passes or violates.
     * The key is a string because the model may reload at any moment
     */
    private final Map<String, RuleState> stateMap = new HashMap<>();
    private final History history = new History();
    private final List<RuleSnapshot> snapshots = new ArrayList<>();

    /**
     * Update the static state with new results from an evaluation.
     *
     * @param states the new results
     */
    void update(Set<RuleState> states, Set<Rule> rules, FHEMModel model) {
        for (RuleState state : states) {

            Rule rule = rules.stream().filter(r -> r.getName().equals(state.getRuleName())).findAny().orElseThrow(() ->
                    new RuntimeException("Rule " + state.getRuleName() + " not found in rules!")
            );

            /* Ignore invisible rules and general predicates, which both should only serve as
             * requisites for real rules.
             */
            if (!rule.isVisible() || rule instanceof GeneralPredicate) {
                continue;
            }

            boolean isOk = state.isOk();

            if (!stateMap.containsKey(rule.getName())) {
                stateMap.put(rule.getName(), state);
                continue;
            }

            if (isOk) {
                /* Was the rule ok previously? */
                if (stateMap.get(rule.getName()).isOk()) {
                    /* If so, do nothing, the stamp is preserved. */
                    continue;
                }
                /* Else, the rule changed to ok in this evaluation.
                 * It has to be added as a new event.
                 */
                RuleEvent event = new RuleEvent(stateMap.get(rule.getName()), rule);
                history.add(event);
                /* Remove the !isOk RuleState and replace it */
                stateMap.put(rule.getName(), state);
            } else {
                /* The rule was not ok, there are violated sensors */
                if (stateMap.get(rule.getName()).isOk()) {
                    /* Previously, rule was ok.
                     * Replace it with freshly stamped notOk state.
                     */
                    stateMap.put(rule.getName(), state);
                } else {
                    /* Rule was not ok and has not changed, so don't do anything for now */
                    continue;
                }
            }
        }
        updateSnapshot(rules);
    }

   /**
     * Attach RuleInfos to all sensors with current warning messages.
     *
     * @param model the model which should be annotated. RuleInfos will be added for the sensors.
     */
    void apply(FHEMModel model, Set<Rule> rules) {
        //prune(model);
        model.setHistory(history);
        model.applyInfo(stateMap);
        model.addStateSnapshot(snapshots);
    }

    private void updateSnapshot(Set<Rule> rules) {
        snapshots.clear();
        for (RuleState state : stateMap.values()) {
            if (state.isOk()) {
                continue;
            }
            Rule rule = rules.stream().filter(r -> r.getName().equals(state.getRuleName())).findAny().orElseThrow(() ->
                    new RuntimeException("Rule from statemap not found: " + state.getRuleName())
            );
            RuleSnapshot snapshot = new RuleSnapshot(state, rule);
            snapshots.add(snapshot);
        }
        snapshots.sort(Comparator.comparingInt(RuleSnapshot::getPriority));
    }

    /**
     * Reset the static state.
     * Useful for unit tests.
     */
    void clear() {
        stateMap.clear();
        history.clear();
        snapshots.clear();
    }
}
