import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.LinkedBlockingQueue;

/*
* This Runnable has a reference to a linkedBlockingQueue, it takes event from it and streams it through the
* outputStream that is given to it. If connection is lost, it invokes connectionClosed.
*/
public class EventTransmitter implements Runnable {

    private DistributedTextEditor callback;
    private LinkedBlockingQueue<Object> linkedBlockingQueue;
    private ObjectOutputStream outputStream;


    public EventTransmitter(LinkedBlockingQueue<Object> linkedBlockingQueue, ObjectOutputStream outputStream, DistributedTextEditor callback) {
        this.linkedBlockingQueue = linkedBlockingQueue;
        this.outputStream = outputStream;
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
                MyTextEvent textEvent = (MyTextEvent) linkedBlockingQueue.take();

                outputStream.writeObject(textEvent);
            } catch (IOException e){
                callback.connectionClosed();
                wasInterrupted = true;
                e.printStackTrace();
            } catch (Exception _) {
                wasInterrupted = true;
                _.printStackTrace();
            }
        }
        try {
            outputStream.writeObject(new ScrambleEvent(callback.getScrambleLamportClock(), callback.getAddedClocks()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("I'm the thread running the EventTransmitter, now I die!");
    }
}
