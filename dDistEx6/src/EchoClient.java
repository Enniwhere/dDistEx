import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class EchoClient {

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        String host = "localhost";
        if(args.length > 0) { host = args[0];}
        Echo server = (Echo) Naming.lookup("//" + host + ":" + EchoServer.portNumber + "/dDistEcho");


        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) System.out.println("Server replies: " + server.echo(sc.nextLine()));

    }
}
