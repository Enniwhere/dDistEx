import java.io.IOException;
import java.rmi.registry.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class EchoServer extends UnicastRemoteObject implements Echo {
    public static final int portNumber = 1099;

    public EchoServer() throws RemoteException {
        super();
    }


    @Override
    public String echo(String message) throws RemoteException {
        System.out.println("Server received: " + message);
        return message;
    }

    public static void main(String[] args) throws IOException {
        EchoServer server = new EchoServer();
        Runtime.getRuntime().exec("rmiregistry 1099");
        LocateRegistry.createRegistry(1099);

        Naming.rebind("//localhost:" + portNumber + "/dDistEcho", server);
    }
}

