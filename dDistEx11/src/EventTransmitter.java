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
        while (!wasInterrupted) {
            try {
                MyTextEvent textEvent = documentEventCapturer.take();
                callback.incrementLamportTime();
                textEvent.setTimestamp(callback.getTimestamp());
                textEvent.setSender(callback.getLamportIndex());
                callback.eventHistory.add(textEvent);
                System.out.println("Sent message with timestamp " + callback.getTimestamp()[0] + "," + callback.getTimestamp()[1]);
                outputStream.writeObject(textEvent);
            } catch (IOException e){
                callback.connectionClosed();
                wasInterrupted = true;
            } catch (Exception _) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventTransmitter, now I die!");
    }
}
