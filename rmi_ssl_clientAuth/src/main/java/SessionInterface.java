import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17
 * this interface provides data access functionality to the clients
 */
public interface SessionInterface extends Remote {
    void receiveMessage(String x)
            throws RemoteException;

    void editSensor(int x, Integer[] i)
            throws RemoteException;

    String printTheInt()
            throws RemoteException;
}
