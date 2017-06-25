package WebServer.FHEMParser.fhemConnection;

import java.io.IOException;

/**
 * This interface contains methods to interface with FHEM.
 * In particular, to get the json internal representation, and to run arbitrary perl commands.
 *
 * @author Rafael on 09.06.17.
 */
public interface FHEMConnection {
    /**
     * This method accesses jsonList2 without specifiying arguments or connection details.
     */
    String getJsonList2() throws IOException, FHEMNotFoundException;

    /**
     * This method accesses jsonList2 at a specified port.
     */
    String getJsonList2(int port, String pathToPl) throws IOException, FHEMNotFoundException;

    /** This method runs a perl command in FHEM. It should verify the command before. */
    boolean sendPerlCommand(String command) throws IOException;
}
