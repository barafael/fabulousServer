import javax.net.ssl.SSLServerSocket;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiServer
        extends java.rmi.server.UnicastRemoteObject
        implements RemoteInterface {

    private static final long serialVersionUID = 5186776461749320975L;

     SSLServerSocket serverSocket;
    boolean edit = true;
    String address;
    Registry registry;
    Sensor sen = new Sensor(1, new Integer[]{1, 2, 3, 4, 5});

    public void receiveMessage(String x) throws RemoteException {
        System.out.println(x);
    }

    public boolean receiveObject(Sensor o) throws RemoteException {
        System.out.println(o.attr);
        return true;
    }

    public void editSensor(int x, Integer[] i) {
        this.edit=false;
        this.sen.setAttr(x);
        this.sen.array = i;
        System.out.println("set sen.array to: " + sen.printTheInt());
    }

    public Sensor getSensor() {
        System.out.println("called get Sensor");
        return this.sen;
    }

    public RmiServer() throws RemoteException, IOException {

        super(35444, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));

        /*try{
            address = (InetAddress.getLocalHost()).toString();
        }
        catch(Exception e){
            System.out.println("can't get inet address.");
        }*/
        int port = 35444;
        System.out.println("this address=" + ",port=" + port);


    }

    @Override
    public String printTheInt() throws RemoteException {
        return this.sen.printTheInt();
    }

    static public void main(String args[]) {
        int port = 35444;
        try {

            setSettings();

            RmiServer server = new RmiServer();

            LocateRegistry.createRegistry(port, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));
            System.out.println("RMI registry running on port " + port);


            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("rmiServer", server);

            RemoteSensorInterface stub = (RemoteSensorInterface) server.exportObject(server.sen, port);

            registry.rebind("ServerSensorStub", stub);

        } catch (IOException e) {
            System.out.println("remote exception" + e);
        }



/*
        try {
            RmiServer server = new RmiServer();

            System.out.println("my Sensor attr is: " + server.sen.attr);
            System.out.print("array is: "+server.sen.printTheInt());
int i=0;
            while (true){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
                server.editSensor(42, new Integer[]{42,42,i,42,42});
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        */
    }

    private static void setSettings() {

        String pass = "password";

        System.setProperty("javax.net.ssl.debug", "all");

        System.setProperty("javax.net.ssl.keyStore", "ssl/server-keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "ssl/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);




    }
}
