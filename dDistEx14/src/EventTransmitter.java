import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

/*
* This Runnable has a reference to a linkedBlockingQueue, it takes event from it and streams it through the
* outputStream that is given to it. If connection is lost, it invokes connectionClosed.
*/
public class EventTransmitter implements Runnable {

    private final String index;
    private DistributedTextEditor callback;
    private LinkedBlockingQueue<Object> linkedBlockingQueue;
    private ObjectOutputStream outputStream;
    private boolean hasReceivedScramble = false;


    public EventTransmitter(LinkedBlockingQueue<Object> linkedBlockingQueue, ObjectOutputStream outputStream, String index, DistributedTextEditor callback) {
        this.linkedBlockingQueue = linkedBlockingQueue;
        this.outputStream = outputStream;
        this.index = index;
        this.callback = callback;
    }

    public void run() {


        boolean wasInterrupted = false;
        try {
            outputStream.writeObject(new ScrambleConnectEvent());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!wasInterrupted) {
            try {
                if(!hasReceivedScramble) {
                    Object obj = linkedBlockingQueue.take();
                    outputStream.writeObject(obj);
                    if(obj instanceof ScrambleEvent) {
                        hasReceivedScramble = true;
                    }
                }
                outputStream.writeObject("crash_me_please");
            } catch (InterruptedException e) {
                wasInterrupted = true;
                //Not a harmful Exception
            } catch (SocketException e) {
                wasInterrupted = true;
                //Not a harmful Exception
            } catch (Exception e) {
                wasInterrupted = true;
                e.printStackTrace();
            }
        }

    }
}
