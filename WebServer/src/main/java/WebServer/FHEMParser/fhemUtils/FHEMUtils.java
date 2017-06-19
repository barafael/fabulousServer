package WebServer.FHEMParser.fhemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * @author Rafael on 13.06.17.
 */
public class FHEMUtils {
    public static int getFHEMPort() {
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

    public static Optional<String> getGlobVar(String var) {
        return Optional.ofNullable(System.getenv(var));
    }

    public static String whereisFhemDotPl() throws IOException {
        Process process = Runtime.getRuntime().exec(new String[] { "bash", "-c", "whereis fhem | sed 's/ /\\n/g' | grep \"fhem.pl\"" });
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        String line = stdin.readLine();
        stdin.close();
        return line;
    }

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
