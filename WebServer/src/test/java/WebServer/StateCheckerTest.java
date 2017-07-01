package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.stateCheck.State;
import WebServer.stateCheck.StateChecker;
import WebServer.stateCheck.rules.BatteryAlert;
import WebServer.stateCheck.rules.LightInTheNight;
import WebServer.stateCheck.rules.WindowOpenLabClosed;
import org.junit.Test;

/**
 * @author Rafael
 */

public class StateCheckerTest {
    @Test
    public void testStateInit() {
        State s = new State();
    }

    @Test
    public void testWindows() {
        StateChecker stateChecker = new StateChecker();
        stateChecker.registerRule(new WindowOpenLabClosed());
        stateChecker.registerRule(new LightInTheNight());
        stateChecker.registerRule(new BatteryAlert());
        stateChecker.evaluate(FHEMParser.getInstance().getFHEMModel().get());
    }
}
