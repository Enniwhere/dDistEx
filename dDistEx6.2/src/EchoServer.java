import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class EchoServer extends UnicastRemoteObject implements Echo {
    private static final int portNumber = 1099;

    public EchoServer() throws RemoteException {
        super();
    }


    @Override
    public String echo(String message) throws RemoteException {
        if(message.equals("/0")) {
            int i = 1/0;
            return "i = " + i;
        }
        if(message.equals("loop")) {
            while(true) {
                //doNothing
            }
        }
        System.out.println("Server received: " + message);
        return message;
    }

    public static void main(String[] args) throws IOException {
        EchoServer server = new EchoServer();
        System.out.println(InetAddress.getLocalHost());
                Runtime.getRuntime().exec("rmiregistry 1099");
        LocateRegistry.createRegistry(1099);
        System.out.println("Contact on this address: "+InetAddress.getLocalHost());

        Naming.rebind("//localhost:" + portNumber + "/dDistEcho", server);
    }
}

