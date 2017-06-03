import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiServer
        extends java.rmi.server.UnicastRemoteObject
        implements RemoteServerInterface {

    private static final long serialVersionUID = 5186776461749320975L;
   static final int port = 35444;


    Sensor sen = new Sensor(1, new Integer[]{1, 2, 3, 4, 5});



   static SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
   static SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory(null, null, true);


    public void receiveMessage(String x) throws RemoteException {
        System.out.println("received message: " + x);
    }

    public void editSensor(int x, Integer[] i) {
        this.sen.setAttr(x);
        this.sen.array = i;
        System.out.println("set sen.array to: " + sen.printTheInt());
    }

    public Sensor getSensor() {
        System.out.println("called get Sensor");
        return this.sen;
    }

    public RmiServer(int port, SslRMIClientSocketFactory csf, SslRMIServerSocketFactory ssf) throws RemoteException, IOException {

        super(port, csf, ssf);
        setSettings();
    }


    public String printTheInt() throws RemoteException {
        return this.sen.printTheInt();
    }


    public SessionInterface login(String u, String password)throws RemoteException{
        boolean registeredUser = false;
        //TODO: get user object from database
        User user = new User(u);

        //TODO: check password in database
        if(u.equals("hans") && password.equals("sonne123")){
            registeredUser= true;
            System.out.println("credentials correct");
        }

        if(registeredUser) {
            Session session = new Session(user,this);
            SessionInterface session_stub = (SessionInterface) this.exportObject(session, port, csf, ssf);
            System.out.println("successfully created Session for user: "+user.getName());
            return session_stub;
        }else{
            throw new RemoteException("Username/Password incorrect. Rejected request!");
        }
    }

    static public void main(String args[]) {
        try {


            RmiServer server = new RmiServer(port, csf, ssf);

            Registry registry = LocateRegistry.createRegistry(port, csf, ssf);
            System.out.println("RMI registry running on port " + port);

            registry.rebind("rmiServer", server);

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
            System.out.println("remote exception" + e);
        }


    }

    private static void setSettings() {

        /*String pass = "password";

        // System.setProperty("javax.net.ssl.debug", "all");

        System.setProperty("javax.net.ssl.keyStore", "../resources/ssl/keystore-server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "../resources/ssl/keystore-client.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);
*/

    }
}
