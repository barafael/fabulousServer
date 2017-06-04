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
 * after successful login it calls data access functions on server
 */
class RmiClient {
    static final private int SERVER_PORT = 35444;
    private Sensor mySen;
    private RemoteServerInterface myPointer;
    private SessionInterface mySession;

    public RmiClient(String serverAddress, int serverPort) {
        this.mySen = new Sensor(0, new Integer[] {0, 0, 0, 0, 0});
        try {
            //setSettings();
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort, new SslRMIClientSocketFactory());
            System.out.println(Arrays.toString(registry.list()));
            this.myPointer = (RemoteServerInterface) (registry.lookup("rmiServer"));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void login(String username, String password) throws RemoteException {

        System.out.println("called client login method");

        this.mySession = this.myPointer.login(new LoginRequest(username, password));
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

        String message1 = "# Some Text1 here #";
        String message2 = "# Some Text2 here #";
        System.out.println("trying to create clients");

        RmiClient client1 = new RmiClient("localhost", SERVER_PORT);
        RmiClient client2 = new RmiClient("localhost", SERVER_PORT);

        try {
            System.out.println("trying to login");
            System.out.println("myPointer1 is: " + client1.myPointer.toString());
            System.out.println("myPointer2 is: " + client2.myPointer.toString());

            client1.login("hans", "sonne123");
            System.out.println("mySession is: "+client1.mySession.toString());
            client2.login("dieter", "mond123");
            System.out.println("mySession is: "+client2.mySession.toString());

            System.out.println("sending " + message1);
            client1.mySession.receiveMessage(message1);
            System.out.println("sending " + message2);
            client1.mySession.receiveMessage(message2);


            System.out.println("calling editSensor, status1: " + client1.mySession.printTheInt());
            client1.mySession.editSensor(66, new Integer[] {6, 6, 6, 6, 6});
            System.out.println("after editSensor: " + client1.mySession.printTheInt());
            System.out.println("calling editSensor, status2: " + client2.mySession.printTheInt());
            client2.mySession.editSensor(66, new Integer[] {6, 6, 6, 6, 6});
            System.out.println("after editSensor: " + client2.mySession.printTheInt());

            /* infinite loop for testing purpose */
            while (true) {
                try {
                    Thread.sleep(2000);
                    System.out.println("in loop1: " + client1.mySession.printTheInt());
                    System.out.println("in loop2: " + client2.mySession.printTheInt());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        } catch (RemoteException e) {
            //TODO: differentiate various Exceptions
            // noSuchObject -> session recycled
            // LoginException -> auth failure
            System.out.println("login failed or connection lost");
            e.printStackTrace();
        }
    }
}
