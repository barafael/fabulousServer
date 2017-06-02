import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface
        extends Remote {

    void receiveMessage(String x)
            throws RemoteException;

    boolean receiveObject(Sensor o)
            throws RemoteException;

    void editSensor(int x, Integer[] i)
            throws RemoteException;

    String printTheInt()
            throws RemoteException;

}
