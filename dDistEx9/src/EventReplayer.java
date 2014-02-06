import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

    private ObjectInputStream inputStream;
    private JTextArea area;
    private DistributedTextEditor distributedTextEditor;

    public EventReplayer(ObjectInputStream inputStream, JTextArea area, DistributedTextEditor distributedTextEditor) {
        this.inputStream = inputStream;
        this.area = area;
        this.distributedTextEditor = distributedTextEditor;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {

            try {
                Object obj = inputStream.readObject();
                if (obj instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent)obj;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                area.insert(tie.getText(), tie.getOffset());
                            } catch (Exception e) {
                                System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the 
				     * EDT unwind, which is now healthy.
				     */
                            }
                        }
                    });
                } else if (obj instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent)obj;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
                            } catch (Exception e) {
                                System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the 
				     * EDT unwind, which is now healthy.
				     */
                            }
                        }
                    });
                }
            } catch (IOException e){
                distributedTextEditor.connectionClosed();
            } catch (Exception _) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

}
