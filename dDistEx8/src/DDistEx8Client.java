

import java.net.*;
import java.io.*;

/**
 *
 * A very simple client which will connect to a server, read from a prompt and
 * send the text to the server.
 */

public class DDistEx8Client {

    /*
     * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3) 
     * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
     * use the port number 40103. This will avoid the unfortunate situation that you
     * connect to each others servers.
     */
    protected int portNumber = 40101;

    /**
     *
     * Will print out the IP address of the local host on which this client runs.
     */
    protected void printLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String localhostAddress = localhost.getHostAddress();
            System.out.println("I'm a client running with IP address " + localhostAddress);
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
    }

    /**
     *
     * Connects to the server on IP address serverName and port number portNumber.
     */
    protected Socket connectToServer(String serverName) {
        Socket res = null;
        try {
            res = new Socket(serverName,portNumber);
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    public void run(String serverName) {
        System.out.println("Hello world!");
        System.out.println("Type CTRL-D to shut down the client.");

        printLocalHostAddress();

        final Socket socket = connectToServer(serverName);

        Runnable sender = new Runnable() {
            @Override
            public void run() {
                try {
                    // For reading from standard input
                    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                    // For sending text to the server
                    ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());
                    String s;
                    // Read from standard input and send to server
                    // Ctrl-D terminates the connection
                    System.out.print("Type a question for the server> ");
                    while ((s = stdin.readLine()) != null) {
                        System.out.print("Type a question for the server> ");
                        QA qa = new QA();
                        qa.setQuestion(s);
                        toServer.writeObject(qa);
                    }
                } catch (IOException e) {
                    // We ignore IOExceptions
                }
            }
        };

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Object obj;
                    // Read and print what the server is sending
                    while ((obj = objectInputStream.readObject()) != null) { // Ctrl-D terminates the connection
                        if (obj instanceof QA){
                            QA qa = (QA) obj;
                            System.out.println("\nThe answer to " + qa.getQuestion() + " is " + qa.getAnswer());
                            System.out.print("Type a question for the server> ");
                        }
                    }
                } catch (IOException e) {
                    // We ignore IOExceptions
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        };

        Thread senderThread = new Thread(sender);
        Thread listenerThread = new Thread(listener);
        if (socket != null) {
            System.out.println("Connected to " + socket);

            senderThread.start();
            listenerThread.start();
            while(senderThread.isAlive() && listenerThread.isAlive()){}
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Goodbye world!");
        }



    }

    public static void main(String[] args) throws IOException {
        DDistEx8Client client = new DDistEx8Client();
        client.run(args[0]);
    }

}
