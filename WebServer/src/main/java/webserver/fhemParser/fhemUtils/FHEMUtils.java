package webserver.fhemParser.fhemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * This class provides methods to interact with FHEM, like getters for global variables and a 'whereis'-lookup.
 *
 * @author Rafael on 13.06.17.
 */
public final class FHEMUtils {
    /**
     * Constructor to prevent creation of utility class.
     */
    private FHEMUtils() {
    }

    /**
     * This private helper method gets any environment variable.
     *
     * @param var the name of the variable to get
     * @return the value of the environment variable var
     */
    public static Optional<String> getGlobVar(String var) {
        return Optional.ofNullable(System.getenv(var));
    }

    /**
     * Accessor for the $FHEMPORT environment variable
     *
     * @return the value of the $FHEMPORT environment variable
     */
    public static int getFHEMPort() {
        /* use getGlobVar here */
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

    /**
     * Execute a 'whereis' command to find fhem.pl
     *
     * @return the path to fhem.pl, including the script name
     * @throws IOException if invocation fails
     */
    private static String whereisFhemDotPl() throws IOException {
        /* The output of 'whereis' is broken into lines, and the one containing the script is returned */
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "whereis fhem | sed 's/ /\\n/g' | grep \"fhem.pl\""});
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        String line = stdin.readLine();
        stdin.close();
        return line;
    }

    /**
     * Gets the value for the FHEM location set as a global variable
     *
     * @return a path to the fhem perl script
     */
    public static String getFhemScriptPath() {
        Optional<String> fhemdir = getGlobVar("FHEMDIR");
        if (fhemdir.isPresent()) {
            return fhemdir.get() + "fhem.pl";
        } else {
            try {
                return whereisFhemDotPl();
            } catch (IOException ioe) {
                System.err.println("Is FHEM installed?");
                return "/usr/bin/fhem.pl";
            }
        }
    }
}
