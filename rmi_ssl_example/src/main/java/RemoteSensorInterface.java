import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by jo on 02.06.17.
 */
public interface RemoteSensorInterface
    extends Remote {
     void setAttr(int c) throws RemoteException;
     String printTheInt() throws RemoteException;
    }
