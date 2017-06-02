import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class RmiClient {
    //Sensor mySen = new Sensor(0,new Integer[]{0,0,0,0,0});
    RemoteSensorInterface myPointer;

    public RmiClient(String serverAddress,String serverPort){
        RemoteInterface rmiServer;
        Registry registry;
        String text = "message";
        try {

            setSettings();

            registry = LocateRegistry.getRegistry("DAVE-PC", (new Integer(serverPort)).intValue(), new SslRMIClientSocketFactory());



            //registry = LocateRegistry.getRegistry(serverAddress, (new Integer(serverPort)).intValue());
            rmiServer = (RemoteInterface) (registry.lookup("rmiServer"));
            // call the remote method
            System.out.println("sending " + text + " to " + serverAddress + ":" + serverPort);
            rmiServer.receiveMessage(text); // test ob verbindung steht


            System.out.println("listed ojects: "+Arrays.toString(registry.list()));

            //System.out.println("on startup: " + myPointer.printTheInt());

            myPointer = (RemoteSensorInterface) registry.lookup("ServerSensorStub");
            System.out.println("after lookup: "+ myPointer.printTheInt());

            //mySen = (Sensor) registry.lookup("ServerSensor");
            //System.out.println("after lookup: " + mySen.printTheInt());

            rmiServer.editSensor(66, new Integer[]{6, 6, 6, 6, 6});
            System.out.println("after editSensor: " + myPointer.printTheInt());


           // mySen = (Sensor) registry.lookup("ServerSensor");
           // System.out.println("again lookup: " + mySen.printTheInt());

            while (true){
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
        System.setProperty("javax.net.ssl.debug", "all");
        System.setProperty("javax.net.ssl.keyStore", "ssl/client-keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "ssl/server-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);

    }

    static public void main(String args[]) {


        RmiClient client = new RmiClient("localhost","35444");


    }
}