package WebServer;

import WebServer.FHEMParser.FHEMParser;
import WebServer.stateCheck.State;
import WebServer.stateCheck.StateChecker;
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
    }
}
