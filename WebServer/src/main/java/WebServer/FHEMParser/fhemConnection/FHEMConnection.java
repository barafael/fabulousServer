package WebServer.FHEMParser.fhemConnection;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Rafael on 09.06.17.
 */
public interface FHEMConnection {
    String getJsonList2() throws IOException, FHEMNotFoundException;

    String getJsonList2(int port, String pathToPl) throws IOException, FHEMNotFoundException;

    Optional<String> sendPerlCommand(String command) throws IOException;
}
