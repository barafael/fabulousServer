package FHEMConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Rafael on 09.06.17.
 */
public class FHEMClientModeCon implements FHEMConnection {
    @Override
    public String getJsonList2() throws IOException {

        String dir_env = "FHEMPL";
        String port_env = "FHEMPORT";
        String path = System.getenv(dir_env);
        String port_str = System.getenv(port_env);
        int port;
        if (path != null) {
            System.out.format("%s=%s%n",
                    dir_env, path);
        } else {
            path = "/usr/bin/";
        }
        if (port_str != null && port_str.matches("\\d+")) {
            System.out.format("%s=%s%n",
                    dir_env, port_str);
            port = Integer.parseInt(port_str);
        } else { port = 7072; }
        return getJsonList2(port, path);
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
        System.err.println(error);
        return stringBuilder.toString();
    }

    @Override
    public Optional<String> sendPerlCommand(String command) {
        return Optional.of(command + ": ACCESS DENIED!!!");
    }
}
