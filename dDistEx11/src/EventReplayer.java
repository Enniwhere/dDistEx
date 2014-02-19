import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;

/*
 * This eventReplayer uses events from its inputStream and replays them in the textfield in the editor
 */
public class EventReplayer implements Runnable {

    private ObjectInputStream inputStream;
    private final JTextArea area;
    private DistributedTextEditor callback;
    private DistributedDocument areaDocument;

    public EventReplayer(ObjectInputStream inputStream, final JTextArea area, DistributedTextEditor callback) {
        this.inputStream = inputStream;
        this.area = area;
        this.callback = callback;
        if (area.getDocument() instanceof DistributedDocument)
            this.areaDocument = ((DistributedDocument)area.getDocument());
        else
            this.areaDocument = null;

    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {

            try {
                Object obj = inputStream.readObject();
                System.out.println("Object received");
                if (obj instanceof  MyConnectionEvent){
                    handleConnectionEvent((MyConnectionEvent) obj);

                } else if (obj instanceof TextInsertEvent) {
                    final TextInsertEvent textInsertEvent = (TextInsertEvent)obj;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                if (areaDocument != null){
                                    callback.eventHistory.add(textInsertEvent);
                                    double[] timestamp = textInsertEvent.getTimestamp();
                                    int senderIndex = textInsertEvent.getSender();
                                    callback.adjustVectorClock(timestamp);
                                    while (timestamp[senderIndex] != callback.getLamportTime(senderIndex)+1 ||
                                           timestamp[callback.getLamportIndex()] > callback.getLamportTime(callback.getLamportIndex())){
                                        Thread.sleep(100);
                                    }

                                    synchronized (areaDocument){
                                        int receiverIndex = callback.getLamportIndex();
                                        ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp[receiverIndex],callback.getLamportTime(receiverIndex), receiverIndex);
                                        LamportTimeComparator comparator = new LamportTimeComparator(receiverIndex);
                                        Collections.sort(historyInterval,comparator);
                                        for (MyTextEvent event : historyInterval){
                                            if (event.getOffset() <= textInsertEvent.getOffset()){
                                                textInsertEvent.setOffset(textInsertEvent.getOffset() + event.getTextLengthChange());
                                            }
                                        }
                                        areaDocument.disableFilter();
                                        area.insert(textInsertEvent.getText(), textInsertEvent.getOffset());
                                        areaDocument.enableFilter();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
				    /* We catch all exceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                            }
                        }
                    });
                } else if (obj instanceof TextRemoveEvent) {
                    final TextRemoveEvent textRemoveEvent = (TextRemoveEvent)obj;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                if (areaDocument != null){
                                    callback.eventHistory.add(textRemoveEvent);
                                    callback.adjustVectorClock(textRemoveEvent.getTimestamp());
                                    double[] timestamp = textRemoveEvent.getTimestamp();
                                    int senderIndex = textRemoveEvent.getSender();
                                    callback.adjustVectorClock(timestamp);
                                    while (timestamp[senderIndex] != callback.getLamportTime(senderIndex)+1 ||
                                           timestamp[callback.getLamportIndex()] > callback.getLamportTime(callback.getLamportIndex())){
                                        //wait
                                    }

                                    synchronized (areaDocument){
                                        int receiverIndex = callback.getLamportIndex();
                                        ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp[receiverIndex],callback.getLamportTime(receiverIndex), receiverIndex);
                                        LamportTimeComparator comparator = new LamportTimeComparator(receiverIndex);
                                        Collections.sort(historyInterval,comparator);
                                        for (MyTextEvent event : historyInterval){
                                            if (event.getOffset() <= textRemoveEvent.getOffset()){
                                                textRemoveEvent.setOffset(textRemoveEvent.getOffset() + event.getTextLengthChange());
                                            }
                                        }
                                        areaDocument.disableFilter();
                                        area.replaceRange(null, textRemoveEvent.getOffset(), textRemoveEvent.getOffset()+textRemoveEvent.getLength());
                                        areaDocument.enableFilter();
                                    }
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
				    /* We catch all exceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                            }
                        }
                    });
                }
            } catch (IOException e){
                callback.connectionClosed();
                wasInterrupted = true;
            } catch (Exception _) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

    private void handleConnectionEvent(MyConnectionEvent obj) {

        if (obj.getType() == ConnectionEventTypes.DISCONNECT_REQUEST){
            callback.replyToDisconnect();
        } else if (obj.getType() == ConnectionEventTypes.DISCONNECT_REPLY_OK){
            callback.connectionClosed();
        }
    }

}
