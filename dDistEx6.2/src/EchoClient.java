import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class EchoClient {
    private static final int portNumber = 1099;
    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        String host = "localhost";
        if(args.length > 0) { host = args[0];}
        Echo server = (Echo) Naming.lookup("//" + host + ":" + portNumber + "/dDistEcho");

        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.equals("Test")) {
                long l = System.currentTimeMillis();
                for(int i=0; i<9999; i++) {
                    server.echo("TESTING TIME");
                }
                System.out.println((l - System.currentTimeMillis())/9999);
            }
            else {
                System.out.println("Server replies: " + line);
            }
        }


    }
}
