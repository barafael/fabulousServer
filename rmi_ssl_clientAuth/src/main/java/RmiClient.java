import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 * tries to connect to RMIserver and invokes login with test user
 * after successfull login it calls data access functions on server
 */
public class RmiClient {
    private Sensor mySen;
    private RemoteServerInterface myPointer;
    private SessionInterface mySession;


    public RmiClient(String serverAddress, int serverPort) {
        this.mySen = new Sensor(0, new Integer[]{0, 0, 0, 0, 0});
        try {
            //setSettings();
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort, new SslRMIClientSocketFactory());
            System.out.println(Arrays.toString(registry.list()));
            this.myPointer = (RemoteServerInterface) (registry.lookup("rmiServer"));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println(e);
        }
    }

    public void login(String username, String password) throws RemoteException {

        System.out.println("called client login method");

        this.mySession = (SessionInterface) this.myPointer.login(username, password);
        if (this.mySession != null) {
            System.out.println("got mySession!");
        } else {
            throw new RemoteException("empty mySession");
        }
    }

    private static void setSettings() {
        //TODO: currently not used
        String pass = "password";
        System.setProperty("javax.net.ssl.debug", "all");
        System.setProperty("javax.net.ssl.keyStore", "../resources/ssl/keystore-client.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "../resources/ssl/keystore-server.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);
    }

    static public void main(String args[]) throws RemoteException {

        String message = "# Some Text here #";
        System.out.println("trying to create client");

        RmiClient client = new RmiClient("localhost", 35444);

        try {
            System.out.println("trying to login");
            System.out.println("myPointer is: " + client.myPointer.toString());

            client.login("hans", "sonne123");

            System.out.println("sending " + message);
            client.mySession.receiveMessage(message);


            System.out.println("calling editSensor, status: " + client.mySession.printTheInt());
            client.mySession.editSensor(66, new Integer[]{6, 6, 6, 6, 6});
            System.out.println("after editSensor: " + client.mySession.printTheInt());

            //infinite loop for testing purpose
            while (true) {
                try {
                    Thread.sleep(3000);
                    System.out.println("in loop: " + client.mySession.printTheInt());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (RemoteException e) {
            System.out.println("login failed");
        }
    }
}