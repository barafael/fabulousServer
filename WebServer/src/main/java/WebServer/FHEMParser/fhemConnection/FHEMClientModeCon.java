package WebServer.FHEMParser.fhemConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import WebServer.FHEMParser.fhemUtils.FHEMUtils;

/**
 * This class contains methods which can interface to FHEM from the computer they are running on.
 * The Client Mode, provided by FHEM, offers FHEM-perl command execution from a system's shell.
 *
 * @author Rafael on 09.06.17.
 */
public class FHEMClientModeCon implements FHEMConnection {
    /**
     * Accessor method for FHEM's jsonList on the local machine.
     * @return A string with jsonList2's content.
     * @throws IOException
     * @throws FHEMNotFoundException if communication couldn't be established
     */
    @Override
    public String getJsonList2() throws IOException, FHEMNotFoundException {
        /* Maybe keep a copy of jsonlist here and just refresh it once in a while ? */
        String pathToFhemPL = FHEMUtils.getFhemScriptPath();
        int port = FHEMUtils.getFHEMPort();
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

    /**
     * Runs a FHEM command, currently assuming it is not malicious
     * maybe add whitelisting later
     * @param command: String in pure FHEM perl syntax; Nothing else needed
     */
    public boolean sendPerlCommand(String command) throws IOException {
        /* TODO: maybe verify command here */
        boolean permitted = true;
        if (permitted) {
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            BufferedReader stdin = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            String line = stdin.readLine();
            stdin.close();
            /* No news is good news */
            return line == null || line.isEmpty();
        }
        return false;
    }
}
