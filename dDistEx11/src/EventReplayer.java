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

                    final double[] timestamp = textInsertEvent.getTimestamp();
                    int senderIndex = textInsertEvent.getSender();

                    System.out.println("Vector clock before while loop is " + callback.getLamportTime(0) + " and " + callback.getLamportTime(1));
                    while ( timestamp[senderIndex] != callback.getLamportTime(senderIndex) + 1 ||
                            timestamp[callback.getLamportIndex()] > callback.getLamportTime(callback.getLamportIndex())){
                        //System.out.println("Timestamp with value " + timestamp[senderIndex] + " is not equal to lamport time with value " + (callback.getLamportTime(senderIndex) + 1.0));
                        Thread.sleep(1000);
                    }
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                if (areaDocument != null){


                                    synchronized (areaDocument){
                                        int receiverIndex = callback.getLamportIndex();
                                        ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp[receiverIndex],callback.getLamportTime(receiverIndex), receiverIndex);

                                        System.out.println("Got the event history between " + timestamp[receiverIndex] + " and " + callback.getLamportTime(receiverIndex));

                                        LamportTimeComparator comparator = new LamportTimeComparator(receiverIndex);
                                        Collections.sort(historyInterval,comparator);

                                        for (MyTextEvent event : historyInterval){
                                            if (event.getOffset() < textInsertEvent.getOffset()){
                                                System.out.print("Adjusted offset from " + textInsertEvent.getOffset() + " to ");
                                                textInsertEvent.setOffset(textInsertEvent.getOffset() + event.getTextLengthChange());
                                                System.out.print(textInsertEvent.getOffset());
                                                if ( event instanceof TextInsertEvent){
                                                    System.out.println(" from the event inserting " + ((TextInsertEvent) event).getText() + " at offset " + event.getOffset());
                                                }
                                            }
                                        }
                                        //callback.eventHistory.add(textInsertEvent);
                                        callback.adjustVectorClock(timestamp);

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

                    final double[] timestamp = textRemoveEvent.getTimestamp();
                    int senderIndex = textRemoveEvent.getSender();


                    System.out.println("Vector clock before while loop is " + callback.getLamportTime(0) + " and " + callback.getLamportTime(1));
                    while ( timestamp[senderIndex] != callback.getLamportTime(senderIndex)+1 ||
                            timestamp[callback.getLamportIndex()] > callback.getLamportTime(callback.getLamportIndex())){
                        //System.out.println("Timestamp with value " + timestamp[senderIndex] + " is not equal to lamport time with value " + (callback.getLamportTime(senderIndex) + 1.0));
                        Thread.sleep(1000);
                    }
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                if (areaDocument != null){

                                    synchronized (areaDocument){
                                        int receiverIndex = callback.getLamportIndex();
                                        ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp[receiverIndex],callback.getLamportTime(receiverIndex), receiverIndex);
                                        LamportTimeComparator comparator = new LamportTimeComparator(receiverIndex);
                                        Collections.sort(historyInterval,comparator);
                                        for (MyTextEvent event : historyInterval){
                                            if (event.getOffset() < textRemoveEvent.getOffset()){
                                                textRemoveEvent.setOffset(textRemoveEvent.getOffset() + event.getTextLengthChange());
                                            }
                                        }
                                        //callback.eventHistory.add(textRemoveEvent);
                                        callback.adjustVectorClock(timestamp);

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
                e.printStackTrace();
                callback.connectionClosed();
                wasInterrupted = true;
            } catch (Exception _) {
                _.printStackTrace();
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
