import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class RmiClient {
    private Sensor mySen;
    private RemoteServerInterface myPointer;
    private SessionInterface mySession;



    public void login(String user, String password) throws RemoteException {

        System.out.println("called client login method");

        this.mySession = (SessionInterface) this.myPointer.login(user,password);
        if(this.mySession != null){
            System.out.println("got mySession!");
        }else{
            throw new RemoteException("empty mySession");
        }
    }

    public RmiClient(String serverAddress, int serverPort) {
        this.mySen  = new Sensor(0, new Integer[]{0, 0, 0, 0, 0});
        try {
            setSettings();
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort, new SslRMIClientSocketFactory());
            System.out.println(Arrays.toString(registry.list()));
            myPointer = (RemoteServerInterface) (registry.lookup("rmiServer"));
            this.mySession = (SessionInterface) myPointer.login("hans","sonne123");
            System.out.println("myPointer is: "+myPointer.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println(e);
        }
    }

    private static void setSettings() {

       /* String pass = "password";
        System.setProperty("javax.net.ssl.debug", "all");
        System.setProperty("javax.net.ssl.keyStore", "../resources/ssl/keystore-client.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "../resources/ssl/keystore-server.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);
*/
    }

    static public void main(String args[]) throws RemoteException {

        String text = "# Some Text here #";
        System.out.println("trying to create client");
        RmiClient client = new RmiClient("localhost", 35444);

        System.out.println(client.mySen.printTheInt());

        //System.out.println("trying to login");
        //client.login("hansdieter","sonne123-falsch");


        System.out.println("sending " + text);
        client.mySession.receiveMessage(text); // test ob verbindung steht

        System.out.println("befor editSensor: " + client.mySession.printTheInt());
            client.mySession.editSensor(66, new Integer[]{6, 6, 6, 6, 6});
            System.out.println("after editSensor: " + client.mySession.printTheInt());

        while (true) {
            try {
                Thread.sleep(3000);
                System.out.println("in loop: " + client.mySession.printTheInt());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}