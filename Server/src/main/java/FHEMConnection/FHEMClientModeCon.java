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
        String[] command = {"perl", pathToPl, "localhost:" + port, "jsonList2"};

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stderr = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String line;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = stdin.readLine()) != null) {
            stringBuilder.append(line);
        }
        StringBuilder error = new StringBuilder();
        while ((line = stderr.readLine()) != null) {
            error.append(line);
        }
        System.out.println(error);
        return stdin.toString();
    }

    @Override
    public Optional<String> sendPerlCommand(String command) {
        return Optional.of(command + ": ACCESS DENIED!!!");
    }
}
