import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteServerInterface
        extends Remote {

    void receiveMessage(String x)
            throws RemoteException;

    void editSensor(int x, Integer[] i)
            throws RemoteException;

    String printTheInt()
            throws RemoteException;

}
