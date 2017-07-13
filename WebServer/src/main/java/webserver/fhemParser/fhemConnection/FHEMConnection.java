package webserver.fhemParser.fhemConnection;

import java.io.IOException;
import java.util.Optional;

/**
 * This interface contains methods to interface with FHEM.
 * In particular, to get the json internal representation, and to run arbitrary perl commands.
 *
 * @author Rafael on 09.06.17.
 */
public interface FHEMConnection {
    /**
     * This method accesses jsonList2 without specifying arguments or connection details.
     * It uses default parameters to find FHEM.
     *
     * @return the raw jsonList2
     *
     * @throws IOException           if there was an error during I/O
     * @throws FHEMNotFoundException if FHEM could not be found at the default location
     */
    String getJsonList2() throws IOException, FHEMNotFoundException;

    /**
     * This method accesses jsonList2 at specified location parameters.
     *
     * @param port       the port to use
     * @param pathToFHEM the path to use
     * @return the raw jsonList2
     *
     * @throws IOException           if there was an error during I/O
     * @throws FHEMNotFoundException if FHEM could not be found at the specified location
     */
    String getJsonList2(int port, String pathToFHEM) throws IOException, FHEMNotFoundException;

    /**
     * This method runs a perl command in FHEM. It does not verify the command before.
     * This should be done in a layer above.
     *
     * @param command the command which should be executed
     * @return the return stdout from the FHEM command, most often empty ('no news is good news')
     *
     * @throws IOException if there was an error during I/O
     */
    Optional<String> execCommand(String command) throws IOException;

    /**
     * Executes a perl command in FHEM
     *
     * @param command the FHEM perl command to execute
     * @return the return stdout from the FHEM command, most often empty ('no news is good news')
     *
     * @throws IOException if there was an error during I/O
     */
    boolean perlCommand(String command) throws IOException;
}
