package fhemConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import fhemUtils.FHEMUtils;
/**
 * @author Rafael on 09.06.17.
 */
public class FHEMClientModeCon implements FHEMConnection {
    @Override
    public String getJsonList2() throws IOException, FHEMNotFoundException {
        return getJsonList2(fhemUtils.FHEMUtils.getFHEMPort(), FHEMUtils.getFhemScriptPath());
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
        stdin.close();
        stderr.close();
        return stringBuilder.toString();
    }

    @Override
    public Optional<String> sendPerlCommand(String command) {
        return Optional.of(command + ": ACCESS DENIED!!!");
    }
}
