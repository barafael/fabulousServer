package webserver.fhemParser.fhemConnection;

import webserver.fhemParser.fhemUtils.FHEMUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * This class contains methods which can interface to FHEM from the computer they are running on.
 * The Client Mode, provided by FHEM, offers FHEM-perl command execution from a system's shell.
 *
 * @author Rafael on 09.06.17.
 */
public final class FHEMClientModeCon implements FHEMConnection {
    /**
     * Path to a fhem.pl location on the local machine.
     */
    private String path;

    /**
     * Port on the local machine which FHEM uses for telnet and client mode.
     */
    private int port;

    /**
     * Constructor which initializes this connection with a given path and port.
     *
     * @param path path to fhem.pl on this system
     * @param port port to use in client mode calls to fhem
     */
    public FHEMClientModeCon(String path, int port) {
        this.path = path;
        this.port = port;
    }

    /**
     * Constructor which initializes this connection with the magic methods in FHEMUtils.
     * (It tries to guess based on `whereis` and global variables in ~/.profile)
     */
    public FHEMClientModeCon() {
        path = FHEMUtils.getFhemScriptPath();
        port = FHEMUtils.getFHEMPort();
    }

    /**
     * Set this connection to use the magic methods in FHEMUtils.
     * (It tries to guess based on `whereis` and global variables in ~/.profile)
     *
     * @return this instance of the * {@link webserver.fhemParser.fhemConnection.FHEMConnection fhem connection},
     * enabling builder style.
     */
    public FHEMClientModeCon useGlobalVariables() {
        path = FHEMUtils.getFhemScriptPath();
        port = FHEMUtils.getFHEMPort();
        return this;
    }

    /**
     * Getter method for FHEM's jsonList on the defined parameters for path and port.
     *
     * @return A string with jsonList2's content.
     *
     * @throws IOException           if the invocation fails
     * @throws FHEMNotFoundException if communication couldn't be established
     */
    @Override
    public String getJsonList2() throws IOException, FHEMNotFoundException {
        return getJsonList2(port, path);
    }

    /**
     * This method gets a jsonList2 from FHEM via the client mode.
     *
     * @param port     the port which should be used
     * @param fhemPath the path at which fhem.pl is located
     * @return the output of the jsonList2 command
     *
     * @throws IOException           if an exception occurred during I/O
     * @throws FHEMNotFoundException if FHEM could not be found at the specified location
     */
    @Override
    public String getJsonList2(int port, String fhemPath) throws IOException, FHEMNotFoundException {
        String[] command = {"perl", fhemPath, "localhost:" + port, "jsonList2"};

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
            // TODO get this out of the if, try to read, if empty ok
            StringBuilder error = new StringBuilder();
            while ((line = stderr.readLine()) != null) {
                error.append(line);
            }
            throw new FHEMNotFoundException(
                    "FHEM not found at " + fhemPath + " on port " + port + "\n"
                            + error);
        }
        stdin.close();
        stderr.close();
        return stringBuilder.toString();
    }

    /**
     * Runs a bash command, assuming it is not malicious.
     *
     * @param command the command to execute
     * @return the stdout from the command
     *
     * @throws IOException if there was an I/O error
     */
    public Optional<String> execCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        String line = stdin.readLine();
        stdin.close();
        return Optional.of(line == null ? "" : line);
    }

    /**
     * Executes a perl command in FHEM via client mode.
     *
     * @param command the FHEM perl command to execute
     * @return the return stdout from the FHEM command, most often empty ('no news is good news')
     *
     * @throws IOException if there was an error during I/O
     */
    @Override
    public boolean perlCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]
                {"sudo", "-u", "fhem", "perl", path, "localhost:" + port, command});
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        String line = stdin.readLine();
        stdin.close();
        return line == null || line.isEmpty();
    }
}
