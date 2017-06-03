import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class RmiClient {
    Sensor mySen = new Sensor(0, new Integer[]{0, 0, 0, 0, 0});
    RemoteSensorInterface myPointer;

    public RmiClient(String serverAddress, int serverPort) {
        RemoteServerInterface rmiServer;
        Registry registry;
        String text = "# Some Text here #";
        try {

            setSettings();

            registry = LocateRegistry.getRegistry(serverAddress, serverPort, new SslRMIClientSocketFactory());

            rmiServer = (RemoteServerInterface) (registry.lookup("rmiServer"));

            System.out.println("sending " + text + " to " + serverAddress + ":" + serverPort);
            rmiServer.receiveMessage(text); // test ob verbindung steht


            System.out.println("listed objects: " + Arrays.toString(registry.list()));

            myPointer = (RemoteSensorInterface) registry.lookup("ServerSensorStub");
            System.out.println("after lookup: " + myPointer.printTheInt());

            rmiServer.editSensor(66, new Integer[]{6, 6, 6, 6, 6});
            System.out.println("after editSensor: " + myPointer.printTheInt());

            while (true) {
                try {
                    Thread.sleep(3000);
                    System.out.println("in loop: " + myPointer.printTheInt());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println(e);
        }


    }

    private static void setSettings() {

        String pass = "password";
        //System.setProperty("javax.net.ssl.debug", "all");
        System.setProperty("javax.net.ssl.keyStore", "./ssl/keystore-client.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "./ssl/keystore-server.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);

    }

    static public void main(String args[]) {
        RmiClient client = new RmiClient("localhost", 35444);
    }
}