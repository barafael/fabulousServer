import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;

public class RmiServer 
  extends java.rmi.server.UnicastRemoteObject
  implements ReceiveMessageInterface{

    String address;
    Registry registry;

    public void receiveMessage(String x) throws RemoteException{
        System.out.println(x);
    }

    public RmiServer() throws RemoteException{
        /*try{
            address = (InetAddress.getLocalHost()).toString();
        }
        catch(Exception e){
            System.out.println("can't get inet address.");
        }*/
        address = "localhost";
        int port=35444;
        System.out.println("this address=" + address +  ",port=" + port);
        try{
            registry = LocateRegistry.createRegistry(port);
            registry.rebind("rmiServer", this);
        }
        catch(RemoteException e){
            System.out.println("remote exception"+ e);
        }
    }
    static public void main(String args[]){
        try{
            RmiServer server = new RmiServer();
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
