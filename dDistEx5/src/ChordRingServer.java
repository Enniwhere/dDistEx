import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by simon on 1/30/14.
 */
public class ChordRingServer {

    private ChordRing ring;
    protected int portNumber = 40101;
    protected ServerSocket serverSocket;

    public ChordRingServer(int size){
        ring = new ChordRing(size);
    }

    /**
     *
     * Will print out the IP address of the local host and the port on which this
     * server is accepting connections.
     */
    protected void printLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String localhostAddress = localhost.getHostAddress();
            System.out.println("Contact this server on the IP address " + localhostAddress);
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
    }

    /**
     *
     * Will register this server on the port number portNumber. Will not start waiting
     * for connections. For this you should call waitForConnectionFromClient().
     */
    protected void registerOnPort() {
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            serverSocket = null;
            System.err.println("Cannot open server socket on port number" + portNumber);
            System.err.println(e);
            System.exit(-1);
        }
    }

    public void deregisterOnPort() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /**
     *
     * Waits for the next client to connect on port number portNumber or takes the
     * next one in line in case a client is already trying to connect. Returns the
     * socket of the connection, null if there were any failures.
     */
    protected Socket waitForConnectionFromClient() {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    public void run() {
        System.out.println("Chord ring deployed. KILL ALL HUMANS.");

        printLocalHostAddress();

        registerOnPort();

        while (true) {
            Socket socket = waitForConnectionFromClient();

            if (socket != null) {
                System.out.println("Connection from " + socket);
                try {
                    BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String s;
                    // Read and print what the client is sending
                    while ((s = fromClient.readLine()) != null) { // Ctrl-D terminates the connection
                        System.out.println("From the client: " + s);
                    }
                    socket.close();
                } catch (IOException e) {
                    // We report but otherwise ignore IOExceptions
                    System.err.println(e);
                }
                System.out.println("Connection closed by client.");
            } else {
                // We rather agressively terminate the server on the first connection exception
                break;
            }
        }

        deregisterOnPort();

        System.out.println("Goodbuy world!");
    }

    public static void main(String[] args) throws IOException {
        ChordRingServer server = new ChordRingServer(16);
        server.run();
    }




}
