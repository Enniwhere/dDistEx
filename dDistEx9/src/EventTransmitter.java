import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventTransmitter implements Runnable {

    private DistributedTextEditor distributedTextEditor;
    private DocumentEventCapturer documentEventCapturer;
    private ObjectOutputStream outputStream;


    public EventTransmitter(DocumentEventCapturer documentEventCapturer, ObjectOutputStream outputStream, DistributedTextEditor distributedTextEditor) {
        this.documentEventCapturer = documentEventCapturer;
        this.outputStream = outputStream;
        this.distributedTextEditor = distributedTextEditor;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = documentEventCapturer.take();
                outputStream.writeObject(mte);
            } catch (IOException e){
                distributedTextEditor.connectionClosed();
                wasInterrupted = true;
            } catch (Exception _) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventTransmitter, now I die!");
    }
}
