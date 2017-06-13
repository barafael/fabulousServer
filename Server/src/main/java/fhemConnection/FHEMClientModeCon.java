package fhemConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author Rafael on 09.06.17.
 */
public class FHEMClientModeCon implements FHEMConnection {
    private int getFHEMPort() {
        String port_env = "FHEMPORT";
        String port_str = System.getenv(port_env);
        int port;

        if (port_str != null && port_str.matches("\\d+")) {
            port = Integer.parseInt(port_str);
        } else {
            port = 7072;
        }
        return port;
    }

    private String whereisFHEM() throws IOException {
        Process process = Runtime.getRuntime().exec(new String[] { "bash", "-c", "whereis fhem | sed 's/ /\\n/g' | grep \"fhem.pl\"" });
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        String line = stdin.readLine();
        return line;
    }

    private String getFHEMPerlPath() throws IOException {
        String dir_env = "FHEMPLo";
        String path_str = System.getenv(dir_env);
        if (path_str == null) {
            try {
                path_str = whereisFHEM();
            } catch (IOException ioe) {
                System.err.println("Is FHEM installed?");
                path_str = "/usr/bin/fhem.pl";
            }
        }
        return path_str;

    }

    @Override
    public String getJsonList2() throws IOException, FHEMNotFoundException {
        return getJsonList2(getFHEMPort(), getFHEMPerlPath());
    }

    @Override
    public String getJsonList2(int port, String pathToFhemPL) throws IOException, FHEMNotFoundException {
        String[] command = {"perl", pathToFhemPL, "localhost:" + port, "jsonList2"};

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
        if (stringBuilder.toString().isEmpty()) {
            StringBuilder error = new StringBuilder();
            while ((line = stderr.readLine()) != null) {
                error.append(line);
            }
            throw new FHEMNotFoundException(
                    "FHEM not found at " + pathToFhemPL + " on port " + port + "\n" +
                            error);
        }
        return stringBuilder.toString();
    }

    @Override
    public Optional<String> sendPerlCommand(String command) {
        return Optional.of(command + ": ACCESS DENIED!!!");
    }
}
