package FHEMConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author Rafael on 09.06.17.
 */
public class FHEMClientModeCon implements FHEMConnection {
    @Override
    public String getJsonList2() throws IOException {
        return getJsonList2(6062, "/usr/bin/fhem.pl");
    }

    @Override
    public String getJsonList2(int port, String pathToPl) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"perl", pathToPl, "localhost:" + port, "jsonList2"};
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = stdInput.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    @Override
    public Optional<String> sendPerlCommand(String command) {
        return Optional.of(command + ": ACCESS DENIED!!!");
    }
}
