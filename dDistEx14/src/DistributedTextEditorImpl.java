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
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistributedTextEditorImpl extends JFrame implements DistributedTextEditor {
    private JTextArea area1 = new JTextArea(new DistributedDocument(), "", 35, 120);
    private JTextField ipAddress = new JTextField("IP address here");
    private JTextField portNumberTextField = new JTextField("Port number here");
    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));
    private String currentFile = "Untitled";
    private boolean changed = false;
    private boolean connected = false;
    private DocumentEventCapturer documentEventCapturer = new DocumentEventCapturer(this);

    protected ServerSocket serverSocket;
    protected Socket socket;

    // Added Fields.
    private Thread eventBroadcasterThread;
    private LinkedBlockingQueue[] eventTransmitterBlockingQueues = new LinkedBlockingQueue[3];
    private int scrambleLamportClock = 0;
    private String lamportIndex;
    private ArrayList<MyTextEvent> eventHistory = new ArrayList<MyTextEvent>();
    private Map<String, Thread> eventReplayerThreadMap = new HashMap<String, Thread>();
    private Map<String, Integer> vectorClockHashMap = new HashMap<String, Integer>();
    private Map<String, Thread> eventTransmitterMap = new HashMap<String, Thread>();
    private NetworkTopologyHelper networkTopologyHelper = new NetworkTopologyHelper();
    private Thread eventTransmitterThread;
    private Map<String, Integer> addedClocks = new HashMap<String, Integer>();  //TODO: This isnt finished
    private Thread listenThread;
    private Pattern portPattern = Pattern.compile("^0*(?:6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|[1-5][0-9]{4}|[1-9][0-9]{1,3}|[0-9])$");
    private Pattern ipPattern = Pattern.compile("(([0-1][\\d]{2}|[2][0-4][\\d]|25[0-5]|\\d{1,2})\\.){3}([0-1][\\d]{2}|[2][0-4][\\d]|25[0-5]|\\d{1,2})");
    private boolean debugIsOn = false;
    private DistributedDocument area1Document;
    private int portNumber;
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
            if (registerOnPort()) {
                setTitle("I'm listening on " + getLocalHostAddress() + ":" + portNumber);
                area1.setText("");
                lamportIndex = getLocalHostAddress() + ":" + portNumber;
                vectorClockHashMap.put(lamportIndex, 0);
                area1Document.enableFilter();
                eventBroadcasterThread = new Thread(getEventBroadcasterRunnable());
                eventBroadcasterThread.start();
                listenThread = new Thread(createListenRunnable());
                listenThread.start();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                portNumberTextField.setText("");
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
            saveOld();
            area1.setText("");
            setTitle("Connecting to " + getIPAddress() + ":" + getPortNumberTextField() + "...");
            try {
                registerOnPort();
                listenThread = new Thread(createListenRunnable());
                listenThread.start();
                eventBroadcasterThread = new Thread(getEventBroadcasterRunnable());
                eventBroadcasterThread.start();
                socket = new Socket(getIPAddress(), getPortNumberTextField());
                setTitle("Connected to " + getIPAddress() + ":" + getPortNumberTextField());
                lamportIndex = getLocalHostAddress() + ":" + portNumber;
                vectorClockHashMap.put(lamportIndex, 0);
                MyConnectionEvent initConnectionEvent = new InitConnectionEvent(vectorClockHashMap);
                area1Document.enableFilter();
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Outputstream initated");
                outputStream.writeObject(initConnectionEvent);
                System.out.println("Wrote initConnectionEvent");
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                System.out.println("Inputstream initiated");
                Object setupEvent = inputStream.readObject();
                System.out.println("Object received : " + setupEvent);
                if(setupEvent instanceof SetupConnectionEvent) {
                    handleSetupConnection((SetupConnectionEvent) setupEvent);
                } else {
                    disconnectAll();
                    return; //We wanna stop this!
                }
                outputStream.close();
                inputStream.close();
                connected = true;
                Listen.setEnabled(false);
                Connect.setEnabled(false);
                Disconnect.setEnabled(true);
            } catch (Exception ce) {
                ce.printStackTrace();
                setTitle("Disconnected: Failed to connect");
                disconnectAll();
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
            disconnectAll();
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


    Action Debug = new AbstractAction("Debug") {
        public void actionPerformed(ActionEvent e) {
            if (!debugIsOn) {
                setTitle("Debug mode activated");
                debugIsOn = true;
            } else {
                setTitle("Debug mode deactivated");
                debugIsOn = false;
            }
        }
    };


    public DistributedTextEditorImpl() {
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area1.addKeyListener(k1);
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(documentEventCapturer);
        area1Document = (DistributedDocument) area1.getDocument();
        JScrollPane scroll1 = new JScrollPane(area1, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(scroll1, BorderLayout.CENTER);
        content.add(ipAddress, BorderLayout.CENTER);
        content.add(portNumberTextField, BorderLayout.CENTER);
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
        file.add(Debug);
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
        area1Document.disableFilter();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return localhostAddress;
    }

    /*
    Will register this server on the port number portNumberTextField.
     */
    private boolean registerOnPort() {
        if (serverSocket == null) {
            for (int i = 40101; i < 65634; i++) {
                if(isPortAvailable(i)){

                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPortAvailable(int portNum) {
        try {
            serverSocket = new ServerSocket(portNum);
            portNumber = portNum;
            return true;
        } catch (IOException e) {
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
                portNumber = -1;
            } catch (IOException e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }

    //This method is responsible for starting the eventTransmitterThread.
    private void startTransmitting(ObjectOutputStream outputStream, String address, LinkedBlockingQueue<Object> eventTransmitterBlockingQueue) throws IOException {
        EventTransmitter eventTransmitter = new EventTransmitter(eventTransmitterBlockingQueue, outputStream, this);
        eventTransmitterThread = new Thread(eventTransmitter);
        eventTransmitterMap.put(address, eventTransmitterThread);
        eventTransmitterThread.start();
    }

    //This method is responsible for starting the eventReplayerThread
    private void startReceiving(ObjectInputStream inputStream, String address) throws IOException {
        EventReplayer eventReplayer = new EventReplayer(inputStream, area1, this, address);
        Thread eventReplayerThread = new Thread(eventReplayer);
        eventReplayerThreadMap.put(address, eventReplayerThread);
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
        //TODO: This should handle single connection closed (Death certificate)
    }



     
    public int getPortNumberTextField() {
        String portNumberString = portNumberTextField.getText();
        Matcher matcher = portPattern.matcher(portNumberString);
        if (matcher.matches()) return Integer.parseInt(portNumberString);
        return 40101;
    }
     
    public String getIPAddress() {
        String ipAddressString = ipAddress.getText();
        Matcher matcher = ipPattern.matcher(ipAddressString);
        if (matcher.matches()) return ipAddressString;
        return "localhost";
    }


     
    public void replyToDisconnect(String eventReplayerAddress) {
        for(String s : eventReplayerThreadMap.keySet()) {
            if(s.equals(eventReplayerAddress)) {
                Thread t = eventReplayerThreadMap.get(s);
                t.interrupt();
                eventReplayerThreadMap.remove(s);
            }
        }
    }


     
    public int getLamportTime(String index) {
        return vectorClockHashMap.get(index);
    }

     
    public String getLamportIndex() {
        return lamportIndex;
    }

     
    public void incrementLamportTime() {
        synchronized (vectorClockHashMap){
            vectorClockHashMap.put(lamportIndex, getLamportTime(lamportIndex) + 1);
        }
    }

     
    public Map<String, Integer> getTimestamp() {
        return new HashMap<String, Integer>(vectorClockHashMap);
    }

     
    public void adjustVectorClock(Map<String, Integer> hashMap) {
        synchronized (vectorClockHashMap){
            for (String s : hashMap.keySet()) {
                vectorClockHashMap.put(s, Math.max(vectorClockHashMap.get(s), hashMap.get(s)));
            }
        }
    }


    public ArrayList<MyTextEvent> getEventHistoryInterval(MyTextEvent textEvent) {
        ArrayList<MyTextEvent> res = new ArrayList<MyTextEvent>();
        Map<String,Integer> timestamp = textEvent.getTimestamp();

        synchronized (eventHistory) {
            for (MyTextEvent event : eventHistory) {
                boolean shouldAdd = false;
                for (String id : timestamp.keySet()){
                    shouldAdd = shouldAdd || (event.getTimestamp().get(id) > timestamp.get(id));
                }
                shouldAdd = shouldAdd || (event.getSender().equals(textEvent.getSender()) && event.getTimestamp().get(lamportIndex) >= timestamp.get(lamportIndex));
                if (shouldAdd) {
                    res.add(event);
                }
            }
        }
        return res;
    }

     
    public void addEventToHistory(MyTextEvent textEvent) {
        synchronized (eventHistory) {
            eventHistory.add(textEvent);
        }
    }

     
    public boolean isDebugging() {
        return debugIsOn;
    }



    //@return true if something where added to this clock.
    public void addToClock(Map<String, Integer> map) {
        for (String s : map.keySet()) {
            if(!vectorClockHashMap.containsKey(s)) {
                System.out.println("Adding " + s + " to clock");
                vectorClockHashMap.put(s, map.get(s));
                addedClocks.put(s, map.get(s));
            }
        }
    }

     
    public void handleSetupConnection(SetupConnectionEvent setupConnectionEvent) {
        area1Document.disableFilter();
        area1.setText(setupConnectionEvent.getText());
        area1Document.enableFilter();
        vectorClockHashMap = new HashMap<String, Integer>(setupConnectionEvent.getMap());
        System.out.println("THIS IS FOR TAGGINGS" + vectorClockHashMap);
        scrambleNetwork(new ScrambleEvent(setupConnectionEvent.getScrambleLamportClock() + 1, addedClocks));
    }

    private Runnable createListenRunnable() {
        return new Runnable() {
            public void run() {
                while (true) {
                    try {
                        socket = serverSocket.accept();
                        if (socket != null) {
                            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                            Object connectionEvent = inputStream.readObject();
                            if(connectionEvent instanceof MyConnectionEvent) {
                                if(((MyConnectionEvent) connectionEvent).getType().equals(ConnectionEventTypes.SCRAMBLE_CONNECTED)) {
                                    String address = socket.getInetAddress() + ":" + socket.getPort();
                                    System.out.println("Got scramble event, connection from " + address);
                                    startReceiving(inputStream, address);
                                } else if(((MyConnectionEvent) connectionEvent).getType().equals(ConnectionEventTypes.INIT_CONNECTION)) {
                                    System.out.println("Received Ann Init Connect Event");
                                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                                    addToClock(((InitConnectionEvent) connectionEvent).getMap());
                                    MyConnectionEvent setupConnectionEvent = new SetupConnectionEvent(area1.getText(), vectorClockHashMap, scrambleLamportClock);
                                    System.out.println("I made this setupConnectionEvent: " + ((InitConnectionEvent) connectionEvent).getMap());
                                    outputStream.writeObject(setupConnectionEvent);
                                    outputStream.close();
                                    inputStream.close();
                                    scrambleNetwork(new ScrambleEvent(scrambleLamportClock+1, addedClocks));
                                }
                            }
                            setTitle("New Connection");
                            connected = true;
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                        //connectionClosed();
                    }
                }
            }
        };
    }

    private void disconnectAll() {
        Listen.setEnabled(true);
        Connect.setEnabled(true);
        Disconnect.setEnabled(false);
        if (connected) {
            connected = false;
            try {
                socket.close();
                socket = null;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (listenThread != null) {
                listenThread.interrupt();
                listenThread = null;
            }
            for(String s : eventReplayerThreadMap.keySet()) {
                eventReplayerThreadMap.get(s).interrupt();
                eventReplayerThreadMap.remove(s);
            }
            for(String s : eventTransmitterMap.keySet()) {
               eventTransmitterMap.get(s).interrupt();
               eventTransmitterMap.remove(s);
            }
        }
        scrambleLamportClock = 0;
        vectorClockHashMap.clear();
        eventHistory.clear();
        area1Document.disableFilter();
        deregisterOnPort();
        setTitle("Disconnected");
        changed = false;
    }


    public synchronized void scrambleNetwork(ScrambleEvent scrambleEvent) {
        System.out.println("Trying to scramble with the clocks : " + scrambleLamportClock + " and " + scrambleEvent.getScrambleLamportClock());
        if(scrambleEvent.getScrambleLamportClock() > scrambleLamportClock) {
            System.out.println("Starting scramble");
            scrambleLamportClock = scrambleEvent.getScrambleLamportClock();
            for(String s : eventTransmitterMap.keySet()) {
                eventTransmitterMap.get(s).interrupt();
            }
            eventTransmitterMap = new HashMap<String, Thread>();

            ArrayList<String> addresses = networkTopologyHelper.selectThreePeers(lamportIndex, vectorClockHashMap);
            System.out.println("Got following peers: " + addresses);
            for (int i = 0; i < addresses.size(); i++) {
                String s = addresses.get(i);
                String ip = s.substring(0, s.indexOf(":"));
                System.out.println("Connecting to this ip : " + ip);
                int port = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.length()));
                System.out.println("on following port: " + port);
                try {
                    socket = new Socket(ip, port);
                    System.out.println("Starting thread with eventTransmitter #" + i);
                    startTransmitting(new ObjectOutputStream(socket.getOutputStream()), s, eventTransmitterBlockingQueues[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(eventTransmitterMap.size() == 0) {
                disconnectAll();
                setTitle("Lost connection to all peers, left network");
            }
            System.out.println("ScrambleLamportClock has been set to : " + scrambleLamportClock);
        }
    }

    public int getScrambleLamportClock() {
        return scrambleLamportClock;
    }

    public Map<String, Integer> getAddedClocks() {
        Map<String, Integer> res = new HashMap<String, Integer>(addedClocks);
        return res;
    }

    public static void main(String[] args) {
        new DistributedTextEditorImpl();
    }

    public Runnable getEventBroadcasterRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    eventTransmitterBlockingQueues[i] = new LinkedBlockingQueue<Object>();
                }
                while(true) {
                    try {
                        Object object = documentEventCapturer.take();
                        for (int i = 0; i < 3; i++) {
                            eventTransmitterBlockingQueues[i].put(object);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
}