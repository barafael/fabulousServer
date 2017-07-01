package WebServer.FHEMParser.fhemConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

import WebServer.FHEMParser.fhemUtils.FHEMUtils;

/**
 * This class contains methods which can interface to FHEM from the computer they are running on.
 * The Client Mode, provided by FHEM, offers FHEM-perl command execution from a system's shell.
 *
 * @author Rafael on 09.06.17.
 */
public class FHEMClientModeCon implements FHEMConnection {
    /* Maybe keep a copy of jsonlist here and just refresh it once in a while ? */
    private String path;
    private int port;

    /**
     * Constructor which initializes this connection with a given path and port
     * @param path path to fhem.pl on this system
     * @param port port to use in client mode calls to fhem
     */
    public FHEMClientModeCon(String path, int port) {
        this.path = path;
        this.port = port;
    }

    /**
     * Constructor which initializes this connection with the magic methods in FHEMUtils
     * (It tries to guess based on `whereis` and global variables in ~/.profile)
     */
    public FHEMClientModeCon() {
        path = FHEMUtils.getFhemScriptPath();
        port = FHEMUtils.getFHEMPort();
    }

    /**
     * Set this connection to use the magic methods in FHEMUtils
     * (It tries to guess based on `whereis` and global variables in ~/.profile)
     * @return this instance of the fhemconnection, enabling builder style
     */
    public FHEMClientModeCon useGlobalVariables() {
        path = FHEMUtils.getFhemScriptPath();
        port = FHEMUtils.getFHEMPort();
        return this;
    }

    /**
     * Accessor method for FHEM's jsonList on the defined parameters for path and port.
     *
     * @return A string with jsonList2's content.
     * @throws IOException if the invocation fails
     * @throws FHEMNotFoundException if communication couldn't be established
     */
    @Override
    public String getJsonList2() throws IOException, FHEMNotFoundException {
        return getJsonList2(port, path);
    }

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
                    "FHEM not found at " + fhemPath + " on port " + port + "\n" +
                            error);
        }
        stdin.close();
        stderr.close();
        return stringBuilder.toString();
    }

    /**
     * Runs a FHEM command, currently assuming it is not malicious
     * maybe add whitelisting later
     *
     */
    public Optional<String> execCommand(String command) throws IOException {
        /* TODO: maybe verify command here */
        boolean permitted = true;
        if (permitted) {
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            BufferedReader stdin = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            String line = stdin.readLine();
            stdin.close();
            /* No news is good news */
            return Optional.of(line == null ? "" : line);
        }
        System.out.println("Could not talk to fhem");
        return Optional.empty();
    }

    @Override
    public boolean perlCommand(String command) throws IOException {
        /* TODO: maybe verify command here */
        boolean permitted = true;
        if (permitted) {
            Process process = Runtime.getRuntime().exec(new String[]
                    {"sudo", "-u", "fhem", "perl", path, "localhost:" + port, command});
            BufferedReader stdin = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            String line = stdin.readLine();
            stdin.close();
            System.out.println(command);
            /* No news is good news */
            System.out.println("Return from FHEM(should be empty) : " + line);
            return true;
        }
        System.out.println("Could not talk to fhem");
        return false;
    }
}
