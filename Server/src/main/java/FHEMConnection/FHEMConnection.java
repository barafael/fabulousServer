package FHEMConnection;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by @author Rafael on 09.06.17.
 */
public interface FHEMConnection {
    public String getJsonList2() throws IOException;

    String getJsonList2(int port, String pathToPl) throws IOException;

    public Optional<String> sendPerlCommand(String commmand);
}
