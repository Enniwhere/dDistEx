import java.io.IOException;
import java.io.ObjectOutputStream;

/*
* This Runnable has a reference to a documentEventCapturer, it takes event from it and streams it through the
* outputStream that is given to it. If connection is lost, it invokes connectionClosed.
*/
public class EventTransmitter implements Runnable {

    private DistributedTextEditor callback;
    private DocumentEventCapturer documentEventCapturer;
    private ObjectOutputStream outputStream;


    public EventTransmitter(DocumentEventCapturer documentEventCapturer, ObjectOutputStream outputStream, DistributedTextEditor callback) {
        this.documentEventCapturer = documentEventCapturer;
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
                MyTextEvent textEvent = documentEventCapturer.take();
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
