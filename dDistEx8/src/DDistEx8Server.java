import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * A very simple server which will way for a connection from a client and print 
 * what the client sends. When the client closes the connection, the server is
 * ready for the next client.
 */

public class DDistEx8Server {

    /*
     * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3) 
     * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
     * use the port number 40103. This will avoid the unfortunate situation that you
     * connect to each others servers.
     */
    protected int portNumber = 40101;
    protected ServerSocket serverSocket;

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
        System.out.println("Hello world!");

        printLocalHostAddress();

        registerOnPort();

        while (true) {
            Socket socket = waitForConnectionFromClient();

            if (socket != null) {
                System.out.println("Connection from " + socket);
                try {
                    // For reading from standard input
                    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                    // For sending text to the server
                    ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

                    String s;
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Object obj;
                    // Read and print what the client is sending
                    while ((obj = objectInputStream.readObject()) != null) { // Ctrl-D terminates the connection
                        if (obj instanceof QA){
                            QA qa = (QA) obj;
                            System.out.println(qa.getQuestion() + "> ");
                            s = stdin.readLine();
                            qa.setAnswer(s);
                            toClient.writeObject(qa);
                        }
                    }
                    socket.close();
                } catch (IOException e) {
                    // We report but otherwise ignore IOExceptions
                    System.err.println(e);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
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
        DDistEx8Server server = new DDistEx8Server();
        server.run();
    }

}

