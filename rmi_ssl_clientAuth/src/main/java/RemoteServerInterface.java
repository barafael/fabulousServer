import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 * this interface provides login functionality to the clients
 */
public interface RemoteServerInterface
        extends Remote {

    SessionInterface login(LoginRequest loginRequest)
            throws RemoteException;

    //TODO: register method

}
