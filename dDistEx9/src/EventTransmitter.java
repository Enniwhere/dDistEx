import java.io.IOException;
import java.io.ObjectOutputStream;

/*
* This Runnable has a reference to a documentEventCapturer, it takes event from it and streams it through the
* outputStream that is given to it. If connection is lost, it invokes connectionClosed.
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
