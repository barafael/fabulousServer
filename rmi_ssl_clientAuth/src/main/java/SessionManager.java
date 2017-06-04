import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 * manages all RMIclient Sessions, handles multi login
 */
public class SessionManager {
    private ArrayList<Session> sessions;

    public SessionManager(ArrayList<Session> sessions) {
        if (sessions != null) {
            this.sessions = sessions;
        } else {
            this.sessions = new ArrayList<Session>();
        }
    }

    /*
    find existing Session for user and return its stub, or create a new one
     */
    public SessionInterface getSession(User user, RmiServer server) throws RemoteException {
        Session session;
        Optional<Session> matchingSession = sessions.stream().filter(s -> s.getUser().getName().equals(user.getName())).findAny();
        //TODO:
        if (matchingSession.isPresent()) {
            session = matchingSession.get();
            try {
                UnicastRemoteObject.unexportObject(session,false);
                System.out.println("successfully created recycled Session for user: " + user.getName());
            } catch (NoSuchObjectException e) {
                System.out.println("can not unexport session. maybe use force?");
                e.printStackTrace();
            }
        } else {
            session = new Session(user, server);
            this.sessions.add(session);
            System.out.println("successfully created new Session for user: " + user.getName());

        }

        return  (SessionInterface) server.exportObject(session, server.port, server.csf, server.ssf);

    }
}
