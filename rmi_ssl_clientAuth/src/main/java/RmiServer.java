import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 * main RMIserver, publishes login functionality over a RMI_Register
 */
public class RmiServer
        extends java.rmi.server.UnicastRemoteObject
        implements RemoteServerInterface {

    private static final long serialVersionUID = 5186776461749320975L;
    private static final int port = 35444;

    private Sensor sen = new Sensor(1, new Integer[]{1, 2, 3, 4, 5});

    private static final SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
    private static final SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory(null, null, true);

    public RmiServer(int port, SslRMIClientSocketFactory csf, SslRMIServerSocketFactory ssf) throws RemoteException, IOException {
        super(port, csf, ssf);
        //setSettings();
    }

    public void receiveMessage(String x) throws RemoteException {
        System.out.println("received message: " + x);
    }

    public void editSensor(int x, Integer[] i) {
        this.sen.setAttr(x);
        this.sen.array = i;
        System.out.println("set sen.array to: " + sen.printTheInt());
    }

    public String printTheInt() throws RemoteException {
        return this.sen.printTheInt();
    }

    /*
        return ether a RMI_pointer to a valid session object to the calling client or throws some _Exceptions_
     */
    public SessionInterface login(String u, String password) throws RemoteException {
        boolean registeredUser = false;
        //TODO: get user object from database identified by _name_:u
        //TODO: catch already registerd users
        //TODO: create sha-1 of input password and compare it to database hash from user.getPassword()
        User user = new User("hans", "sonne123");

        if (u.equals(user.getName()) && password.equals(user.getPassword())) {
            registeredUser = true;
            System.out.println("credentials correct");
        } else {
            System.out.println("credentials invalid");
        }

        if (registeredUser) {
            Session session = new Session(user, this);
            SessionInterface session_stub = (SessionInterface) this.exportObject(session, port, csf, ssf);
            System.out.println("successfully created Session for user: " + user.getName());
            return session_stub;
        } else {
            //TODO: throw special LoginException (easier to catch in client)
            throw new RemoteException("Username/Password incorrect. Rejected request!");
        }
    }

    static public void main(String args[]) {
        try {
            RmiServer server = new RmiServer(port, csf, ssf);

            Registry registry = LocateRegistry.createRegistry(port, csf, ssf);
            System.out.println("RMI registry running on port " + port);

            //publish login-server on registry
            registry.rebind("rmiServer", server);


            //infinite loop for testing purpose
            int i = 0;
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
                server.editSensor(42, new Integer[]{42, 42, i, 42, 42});
            }

        } catch (IOException e) {
            System.out.println("remote exception: " + e);
        }
    }

    private static void setSettings() {
        //TODO:currently not used, instead used JVM arguments to pass those files (-> fix path and use again)
        String pass = "password";
        // if there are errors containing network, activate following output debug rule
        // System.setProperty("javax.net.ssl.debug", "all");
        System.setProperty("javax.net.ssl.keyStore", "../resources/ssl/keystore-server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "../resources/ssl/keystore-client.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);
    }
}
