import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;

public class RmiClient{
    static public void main(String args[]){
        ReceiveMessageInterface rmiServer;
        Registry registry;
        String serverAddress="localhost"; //args[0];
        String serverPort="35444"; //args[1];
        String text=args[0];  //args[2];
        System.out.println("sending " + text + " to " +serverAddress + ":" + serverPort);
        try{
            registry=LocateRegistry.getRegistry(serverAddress,(new Integer(serverPort)).intValue());
            rmiServer=(ReceiveMessageInterface)(registry.lookup("rmiServer"));
            // call the remote method
            rmiServer.receiveMessage(text);
        }
        catch(RemoteException e){
            e.printStackTrace();
        }
        catch(NotBoundException e){
            System.err.println(e);
        }
    }
}
