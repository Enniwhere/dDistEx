import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistributedTextEditor extends JFrame {
    private JTextArea area1 = new JTextArea(new DistributedDocument(),"",20, 120);
    private JTextArea area2 = new JTextArea(20, 120);
    private JTextField ipAddress = new JTextField("IP address here");
    private JTextField portNumber = new JTextField("Port number here");
    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));
    private String currentFile = "Untitled";
    private boolean changed = false;
    private boolean connected = false;
    private DocumentEventCapturer documentEventCapturer = new DocumentEventCapturer();

    protected ServerSocket serverSocket;
    protected Socket socket;

    // Added Fields
    private int lamportIndex = 0;
    private volatile ObjectOutputStream outputStream;
    private volatile ObjectInputStream inputStream;
    public volatile ArrayList<MyTextEvent> eventHistory = new ArrayList<MyTextEvent>();
    private ArrayList<Double> vectorClockArray = new ArrayList<Double>();
    private EventTransmitter eventTransmitter;
    private Thread eventTransmitterThread;
    private EventReplayer eventReplayer;
    private Thread eventReplayerThread;
    private Thread listenThread;
    private Pattern portPattern = Pattern.compile("^0*(?:6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|[1-5][0-9]{4}|[1-9][0-9]{1,3}|[0-9])$");
    private Pattern ipPattern = Pattern.compile("(([0-1][\\d]{2}|[2][0-4][\\d]|25[0-5]|\\d{1,2})\\.){3}([0-1][\\d]{2}|[2][0-4][\\d]|25[0-5]|\\d{1,2})");
    //end of added fields

    ActionMap m = area1.getActionMap();
    Action Copy = m.get(DefaultEditorKit.copyAction);
    Action Paste = m.get(DefaultEditorKit.pasteAction);

    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
        }
    };


    /*
    Firing the "Listen" action will start a thread that listens for a client to connect.
    The registerOnPort checks if the port could be registered
    When a connection has been established it will start two new threads; "EventTransmitter" and "EventReplayer"
    They are initialised and started in helper-methods at the bottom. Each thread gets a reference to this
    DistributedTextEditor as well as either an input- or outputStream. For the sake of the EventTransmitter
    it will also have a reference to the DocumentEventCapturer to be able to fetch events from it.
     */
    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            Listen.setEnabled(false);
            Connect.setEnabled(false);
            Disconnect.setEnabled(true);
            if(registerOnPort()) {
                setTitle("I'm listening on " + getLocalHostAddress() + ":" + getPortNumber());
                Runnable listener = new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                socket = serverSocket.accept();
                                if (socket != null) {
                                    lamportIndex = 0;
                                    vectorClockArray.add(0,new Double(0));
                                    vectorClockArray.add(1,new Double(0.1));
                                    setTitle(getTitle() + ". Connection established from " + socket);
                                    area2.setText("");
                                    startTransmitting();
                                    startReceiving();
                                    connected = true;
                                }
                            } catch (IOException e) {
                                connectionClosed();
                            }
                        }
                    }
                };
                listenThread = new Thread(listener);
                listenThread.start();
            } else {
                setTitle("Could not register on port, maybe its already registered?");
            }
        }
    };

    /*
    When this action is fired the client will attempt to connect to a given IP and portnumber.
    When the connection is established this DistributedTextEditor will also start two new threads, just as
    the "Listen" action above. If for some reason an exception is thrown there is a small cleanup in the method
    "connectionClosed".
     */
    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            area2.setText("");
            setTitle("Connecting to " + getIPAddress() + ":" + portNumber.getText() + "...");
            try {
                socket = new Socket(getIPAddress(), getPortNumber());
                setTitle("Connected to " + getIPAddress() + ":" + portNumber.getText());
                lamportIndex = 1;
                vectorClockArray.add(0,new Double(0));
                vectorClockArray.add(1,new Double(0.1));
                startTransmitting();
                startReceiving();
                connected = true;
                Listen.setEnabled(false);
                Connect.setEnabled(false);
                Disconnect.setEnabled(true);
            } catch (IOException ex) {
                connectionClosed();
            }
            changed = false;
        }
    };

    /*
    When "Disconnect" is fired the first thing to happens is the Editor deregisters on port,
    If the editor was currently listening (but no connection was made yet) that thread will be interrupted
    Then the helper method connectionClosed is run. connectionClosed can be found at the bottom.
     */
    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            deregisterOnPort();
            if (listenThread != null) {
                listenThread.interrupt();
                listenThread = null;
            }
            connectionClosed();
            setTitle("Disconnected");
            area2.setText("");
            changed = false;
        }
    };

    Action Save = new AbstractAction("Save") {
        public void actionPerformed(ActionEvent e) {
            if (!currentFile.equals("Untitled"))
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


    public DistributedTextEditor() {
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area1.addKeyListener(k1);
        area2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area2.setEditable(false);
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(documentEventCapturer);

        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JScrollPane scroll2 =
                new JScrollPane(area2,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(scroll1, BorderLayout.CENTER);
        content.add(scroll2, BorderLayout.CENTER);
        content.add(ipAddress, BorderLayout.CENTER);
        content.add(portNumber, BorderLayout.CENTER);

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
        setTitle("Disconnected");
        setVisible(true);
        area1.insert("Start listening or connect to a server to use this DistributedTextEditor", 0);


    }

    private void saveFileAs() {
        if (dialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if (changed) {
            if (JOptionPane.showConfirmDialog(this, "Would you like to save " + currentFile + " ?", "Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
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
        } catch (IOException e) {
        }
    }


    // This method is simply used to find the local host address.
    private String getLocalHostAddress() {
        String localhostAddress = "";
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            localhostAddress = localhost.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            localhostAddress = "Cannot resolve the local host address";
        }
        return localhostAddress;
    }


    /*
    Will register this server on the port number portNumber.
     */
    private boolean registerOnPort() {
        if (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(getPortNumber());
                return true;
            } catch (IOException e) {
                serverSocket = null;
            }
        }
        return false;
    }

    /*
    Will deregister the server on the port
     */
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

    //This method is responsible for starting the eventTransmitterThread.
    private void startTransmitting() throws IOException {
        documentEventCapturer.clearEventHistory();
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        MyTextEvent initEvent = new TextInsertEvent(0, area1.getText());
        incrementLamportTime();
        initEvent.setTimestamp(getTimestamp());
        initEvent.setSender(lamportIndex);

        outputStream.writeObject(initEvent);
        eventTransmitter = new EventTransmitter(documentEventCapturer, outputStream, this);
        eventTransmitterThread = new Thread(eventTransmitter);
        eventTransmitterThread.start();
    }

    //This method is responsible for starting the eventReplayerThread
    private void startReceiving() throws IOException {
        inputStream = new ObjectInputStream(socket.getInputStream());
        eventReplayer = new EventReplayer(inputStream, area1, this);
        eventReplayerThread = new Thread(eventReplayer);
        eventReplayerThread.start();
    }

    /*
    This is our cleanup method that is run whenever a connection is closed. It is invoked from the
    eventTransmitter and eventReplayer whenever a connection was lost. It is also invoked from the action
    file -> Disconnect. This method is synchronized to avoid concurrency problems. Theres a difference wether
    the Editor was currently a client or server. A client should disconnect if the server stops responding, but
    the server should keep running when a client disconnect. The transmitter and replayer are intterupted aswell.
    */
    public synchronized void connectionClosed() {
        boolean checkListening = listenThread != null;
        Listen.setEnabled(!checkListening);
        Connect.setEnabled(!checkListening);
        Disconnect.setEnabled(checkListening);
        area2.setText("");
        if (connected) {
            connected = false;
            try {
                socket.close();
                socket = null;
            } catch (IOException e1) {
                // Ignore exceptions
            }
            if (listenThread == null) setTitle("Disconnected");
            else setTitle("I'm listening on " + getLocalHostAddress() + ":" + getPortNumber());
            if (eventReplayerThread != null) {
                eventReplayerThread.interrupt();
                eventReplayerThread = null;
                eventReplayer = null;
            }
            if (eventTransmitterThread != null) {
                eventTransmitterThread.interrupt();
                eventTransmitterThread = null;
                eventTransmitter = null;
            }
            outputStream = null;
            inputStream = null;
        }
    }

    public int getPortNumber() {
        String portNumberString = portNumber.getText();
        Matcher matcher = portPattern.matcher(portNumberString);
        if(matcher.matches()) return Integer.parseInt(portNumberString);
        return 40101;
    }

    public String getIPAddress() {
        String ipAddressString = ipAddress.getText();
        Matcher matcher = ipPattern.matcher(ipAddressString);
        if(matcher.matches()) return ipAddressString;
        return "localhost";
    }


    public void replyToDisconnect(){
        eventTransmitterThread.interrupt();
        eventReplayerThread.interrupt();
        try {
            outputStream.writeObject(new MyConnectionEvent(ConnectionEventTypes.DISCONNECT_REPLY_OK));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connectionClosed();
        }
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

    public synchronized double getLamportTime(int index) {
        return vectorClockArray.get(index);
    }

    public synchronized boolean setLamportTime(double lamportTime,int index) {
        if (this.vectorClockArray.get(index) >= lamportTime){
            return false;
        }
        this.vectorClockArray.set(index,lamportTime);
        return true;
    }

    public int getLamportIndex() {
        return lamportIndex;
    }

    public synchronized void incrementLamportTime() {
        vectorClockArray.set(lamportIndex, getLamportTime(lamportIndex)+1);
    }

    public double[] getTimestamp() {
        double[] timestamp = new double[vectorClockArray.size()];
        for (int i = 0; i < vectorClockArray.size(); i++){
            timestamp[i] = vectorClockArray.get(i);
        }
        return timestamp;
    }

    public void adjustVectorClock(double[] timestamp) {
        for (int i = 0; i < timestamp.length; i++) {
            vectorClockArray.set(i,Math.max(vectorClockArray.get(i),timestamp[i]));
        }
    }

    public ArrayList<MyTextEvent> getEventHistoryInterval(double start, double end, int lamportIndex){
        ArrayList<MyTextEvent> res = new ArrayList<MyTextEvent>();
        for (MyTextEvent event : eventHistory){
            double time = event.getTimestamp()[lamportIndex];
            if (time > start && time <= end){
                res.add(event);
            }
        }
        return res;
    }
}
