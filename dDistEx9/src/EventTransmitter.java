import javax.swing.JTextArea;
import java.awt.EventQueue;
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

    private DocumentEventCapturer dec;
    private ObjectOutputStream outputStream;

    public EventTransmitter(DocumentEventCapturer dec, ObjectOutputStream outputStream) {
        this.dec = dec;
        this.outputStream = outputStream;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = dec.take();
                outputStream.writeObject(mte);
            } catch (Exception _) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventTransmitter, now I die!");
    }
}