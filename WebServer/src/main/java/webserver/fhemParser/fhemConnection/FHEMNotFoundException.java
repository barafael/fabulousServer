package webserver.fhemParser.fhemConnection;

/**
 * This exception signals that no instance of FHEM could be detected
 * and thus, no command could be executed.
 *
 * @author Rafael on 13.06.17.
 */
public class FHEMNotFoundException extends Exception {
    /**
     * Construct an exception with an error message.
     *
     * @param message the message which should state and clarify the error
     */
    public FHEMNotFoundException(String message) {
        super(message);
    }
}
