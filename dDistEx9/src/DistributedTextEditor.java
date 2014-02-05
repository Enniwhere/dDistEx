
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.net.*;
import java.util.Enumeration;

public class DistributedTextEditor extends JFrame {

    // Added code
    protected int listenPort = 40101;
    protected ServerSocket serverSocket;
    protected Socket socket;

    private JTextArea area1 = new JTextArea(20,120);
    private JTextArea area2 = new JTextArea(20,120);
    private JTextField ipaddress = new JTextField("IP address here");
    private JTextField portNumber = new JTextField("Port number here");

    private EventTransmitter eventTransmitter;
    private Thread eventTransmitterThread;
    private EventReplayer eventReplayer;
    private Thread eventReplayerThread;

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private boolean connected = false;
    private DocumentEventCapturer dec = new DocumentEventCapturer();

    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
        }
    };

    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            area1.setText("");

            // Added code
            registerOnPort();
            setTitle("I'm listening on " + getLocalHostAddress() + ":" + listenPort);
            Runnable listener = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            socket = serverSocket.accept();
                            if (socket != null) {
                                setTitle(getTitle() + ". Connection established from " + socket);
                                startTransmitting();
                                startReceiving();
                                connected = true;
                            }
                        } catch (IOException e) {
                            // We ignore IOExceptions
                        }
                    }
                }
            };
            Thread listenThread = new Thread(listener);
            listenThread.start();

            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            area1.setText("");
            setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber.getText() + "...");
            try {
                socket = new Socket(ipaddress.getText(),Integer.parseInt(portNumber.getText()));
                setTitle("Connected to " + ipaddress.getText() + ":" + portNumber.getText());
                startTransmitting();
                startReceiving();
                connected = true;
            } catch (IOException ex) {
                // We ignore IOExceptions
            }
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            setTitle("Disconnected");
            // TODO
        }
    };

    Action Save = new AbstractAction("Save") {
        public void actionPerformed(ActionEvent e) {
            if(!currentFile.equals("Untitled"))
                saveFile(currentFile);
            else
                saveFileAs();
        }
    };

    Action SaveAs = new AbstractAction("Save as...") {
        public void actionPerformed(ActionEvent e) {
            saveFileAs();
        }
    };

    Action Quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            System.exit(0);
        }
    };

    ActionMap m = area1.getActionMap();

    Action Copy = m.get(DefaultEditorKit.copyAction);
    Action Paste = m.get(DefaultEditorKit.pasteAction);

    public DistributedTextEditor() {
        area1.setFont(new Font("Monospaced",Font.PLAIN,12));

        area2.setFont(new Font("Monospaced",Font.PLAIN,12));
        ((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);
        area2.setEditable(false);

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1,BorderLayout.CENTER);

        JScrollPane scroll2 =
                new JScrollPane(area2,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll2,BorderLayout.CENTER);

        content.add(ipaddress,BorderLayout.CENTER);
        content.add(portNumber,BorderLayout.CENTER);

        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMB.add(file);
        JMB.add(edit);

        file.add(Listen);
        file.add(Connect);
        file.add(Disconnect);
        file.addSeparator();
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);

        edit.add(Copy);
        edit.add(Paste);
        edit.getItem(0).setText("Copy");
        edit.getItem(1).setText("Paste");

        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        area1.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);
        area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
                "Try to type and delete stuff in the top area.\n" +
                "Then figure out how it works.\n", 0);


    }

    private void saveFileAs() {
        if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if(changed) {
            if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        }
        catch(IOException e) {
        }
    }


    // Added code
    private String getLocalHostAddress() {
        String localhostAddress = "";
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            localhostAddress = localhost.getHostAddress();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    System.out.println(iface.getDisplayName() + " " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            localhostAddress = "Cannot resolve the local host address";
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return localhostAddress;
    }

    /**
     * Added code
     * Will register this server on the port number portNumber. Will not start waiting
     * for connections. For this you should call waitForConnectionFromClient().
     */
    private void registerOnPort() {
        try {
            serverSocket = new ServerSocket(listenPort);
        } catch (IOException e) {
            serverSocket = null;
            System.err.println("Cannot open server socket on port number" + listenPort);
            System.err.println(e);
            System.exit(-1);
        }
    }

    private void deregisterOnPort() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private void startTransmitting() throws IOException {
        eventTransmitter = new EventTransmitter(dec, new ObjectOutputStream(socket.getOutputStream()));
        eventTransmitterThread = new Thread(eventTransmitter);
        eventTransmitterThread.start();
    }

    private void startReceiving() throws IOException {
        eventReplayer = new EventReplayer(new ObjectInputStream(socket.getInputStream()),area2);
        eventReplayerThread = new Thread(eventReplayer);
        eventReplayerThread.start();
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}
