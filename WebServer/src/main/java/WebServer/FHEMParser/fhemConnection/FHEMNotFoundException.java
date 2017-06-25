package WebServer.FHEMParser.fhemConnection;

/**
 * This exception signals that no instance of FHEM could be detected
 * and thus, no command could be executed.
 * @author Rafael on 13.06.17.
 */
public class FHEMNotFoundException extends Exception {
    public FHEMNotFoundException(String message) {
        super(message);
    }
}
