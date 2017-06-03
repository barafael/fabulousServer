import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteServerInterface
        extends Remote {

    SessionInterface login(String username, String password)
            throws RemoteException;

}
