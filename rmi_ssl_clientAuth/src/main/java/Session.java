import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17
 * checks on user privileges and redirects to data access methods in RmiServer
 */
public class Session implements SessionInterface {

    private User user;
    private long ID;
    private ArrayList<String> privileges;
    private RmiServer server;

    public Session(User u, RmiServer s) {
        this.user = u;
        this.server = s;
        //this.ID = TODO: set stuff
        //this.privileges
    }

    public void receiveMessage(String x) throws RemoteException {
        //TODO: check for user permission around calling method
        server.receiveMessage(x);
    }

    public void editSensor(int x, Integer[] i) throws RemoteException {
        //TODO: check for user permission around calling method
        server.editSensor(x, i);
    }

    public String printTheInt() throws RemoteException {
        //TODO: check for user permission around calling method
        return server.printTheInt();
    }

    public User getUser(){
        return this.user;
    }
}
