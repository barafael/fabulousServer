import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiServer
        extends java.rmi.server.UnicastRemoteObject
        implements RemoteInterface {

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

    public RmiServer() throws RemoteException {
        /*try{
            address = (InetAddress.getLocalHost()).toString();
        }
        catch(Exception e){
            System.out.println("can't get inet address.");
        }*/
        int port = 35444;
        System.out.println("this address=" + ",port=" + port);
        try {
            registry = LocateRegistry.createRegistry(port);
            registry.rebind("rmiServer", this);

            RemoteSensorInterface stub = (RemoteSensorInterface) this.exportObject(sen, port);

            registry.rebind("ServerSensorStub", stub);

        } catch (RemoteException e) {
            System.out.println("remote exception" + e);
        }
    }

    @Override
    public String printTheInt() throws RemoteException {
        return this.sen.printTheInt();
    }

    static public void main(String args[]) {
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
    }
}
