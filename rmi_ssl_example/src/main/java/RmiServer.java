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


    Sensor sen = new Sensor(1, new Integer[]{1, 2, 3, 4, 5});

    public void receiveMessage(String x) throws RemoteException {
        System.out.println(x);
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

        super(port, csf,ssf);
        System.out.println("this address=IP ,port= "+port);
    }

    @Override
    public String printTheInt() throws RemoteException {
        return this.sen.printTheInt();
    }

    static public void main(String args[]) {
        int port = 35444;
        try {

            setSettings();

            SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory(null, null, true);

            RmiServer server = new RmiServer(port,csf,ssf);

            Registry registry = LocateRegistry.createRegistry(port, csf, ssf);
            System.out.println("RMI registry running on port " + port);

            registry.rebind("rmiServer", server);

            RemoteSensorInterface stub = (RemoteSensorInterface) server.exportObject(server.sen, port, csf,ssf);
            registry.rebind("ServerSensorStub", stub);

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

        String pass = "password";

       // System.setProperty("javax.net.ssl.debug", "all");

        System.setProperty("javax.net.ssl.keyStore", "./ssl/keystore-server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", "./ssl/keystore-client.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", pass);


    }
}
