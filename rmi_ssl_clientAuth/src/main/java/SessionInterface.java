import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by jo on 03.06.17.
 */
public interface SessionInterface extends Remote {
    void receiveMessage(String x)
            throws RemoteException;

    void editSensor(int x, Integer[] i)
            throws RemoteException;

    String printTheInt()
            throws RemoteException;
}
