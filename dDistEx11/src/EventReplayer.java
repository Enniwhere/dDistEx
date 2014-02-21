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
                if (callback.isDebugging()) Thread.sleep(1000);      // Debugging purposes
                Object obj = inputStream.readObject();
                System.out.println("Object received");
                if (obj instanceof  MyConnectionEvent){
                    handleConnectionEvent((MyConnectionEvent) obj);

                } else if (obj instanceof TextInsertEvent) {
                    final TextInsertEvent textInsertEvent = (TextInsertEvent)obj;

                    final double[] timestamp = textInsertEvent.getTimestamp();
                    final int senderIndex = textInsertEvent.getSender();

                    System.out.println("Vector clock before while loop is " + callback.getLamportTime(0) + " and " + callback.getLamportTime(1));
                    while ( timestamp[senderIndex] != callback.getLamportTime(senderIndex) + 1 ||
                            timestamp[callback.getLamportIndex()] > callback.getLamportTime(callback.getLamportIndex())){
                        //System.out.println("Timestamp with value " + timestamp[senderIndex] + " is not equal to lamport time with value " + (callback.getLamportTime(senderIndex) + 1.0));
                        Thread.sleep(100);
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
                                        boolean ignore = false;

                                        for (MyTextEvent historyEvent : historyInterval){
                                            int insertEventOffset = textInsertEvent.getOffset();

                                            int historyEventOffset = historyEvent.getOffset();
                                            int historyEventTextLengthChange = historyEvent.getTextLengthChange();

                                            if (historyEventOffset <= insertEventOffset &&
                                                (historyEventOffset != insertEventOffset || receiverIndex < senderIndex)){


                                                if(historyEvent instanceof TextRemoveEvent && historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= insertEventOffset
                                                        && (historyEventOffset +((TextRemoveEvent) historyEvent).getLength() != insertEventOffset || receiverIndex < senderIndex)){

                                                    ignore = true;

                                                } else {

                                                    textInsertEvent.setOffset(insertEventOffset + historyEventTextLengthChange);
                                                }

                                            } else {

                                                historyEvent.setOffset(historyEventOffset + textInsertEvent.getTextLengthChange());

                                            }
                                        }
                                        //callback.eventHistory.add(textInsertEvent);
                                        callback.adjustVectorClock(timestamp);

                                        if (!ignore){
                                            areaDocument.disableFilter();
                                            area.insert(textInsertEvent.getText(), textInsertEvent.getOffset());
                                            areaDocument.enableFilter();
                                        }
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
                    final int senderIndex = textRemoveEvent.getSender();


                    System.out.println("Vector clock before while loop is " + callback.getLamportTime(0) + " and " + callback.getLamportTime(1));
                    while ( timestamp[senderIndex] != callback.getLamportTime(senderIndex)+1 ||
                            timestamp[callback.getLamportIndex()] > callback.getLamportTime(callback.getLamportIndex())){
                        //System.out.println("Timestamp with value " + timestamp[senderIndex] + " is not equal to lamport time with value " + (callback.getLamportTime(senderIndex) + 1.0));
                        Thread.sleep(100);
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
                                            int removeEventOffset = textRemoveEvent.getOffset();
                                            int removeEventTextLengthChange = textRemoveEvent.getTextLengthChange();
                                            int removeEventLength = textRemoveEvent.getLength();

                                            int eventOffset = event.getOffset();
                                            int eventTextLengthChange = event.getTextLengthChange();

                                            if (eventOffset <= removeEventOffset &&
                                                (eventOffset != removeEventOffset || receiverIndex < senderIndex)){

                                                if(event instanceof TextRemoveEvent &&
                                                        removeEventOffset <= eventOffset + ((TextRemoveEvent)event).getLength() &&
                                                        (eventOffset + ((TextRemoveEvent) event).getLength() != removeEventOffset || receiverIndex < senderIndex)){
                                                    textRemoveEvent.setLength(removeEventLength -
                                                                              (eventOffset + ((TextRemoveEvent)event).getLength() -
                                                                               removeEventOffset));

                                                    textRemoveEvent.setOffset(event.getOffset());

                                                } else {
                                                    textRemoveEvent.setOffset(removeEventOffset + eventTextLengthChange);
                                                }

                                            } else if (eventOffset <= removeEventOffset + removeEventLength &&
                                                       (eventOffset != removeEventOffset + removeEventLength || receiverIndex < senderIndex)) {

                                                textRemoveEvent.setLength(removeEventLength + Math.max(eventTextLengthChange,-(removeEventOffset + removeEventLength - eventOffset)));

                                            } else {
                                                event.setOffset(eventOffset + removeEventTextLengthChange);
                                            }

                                        }
                                        //callback.eventHistory.add(textRemoveEvent);
                                        callback.adjustVectorClock(timestamp);

                                        int removeEventOffset = textRemoveEvent.getOffset();
                                        int removeEventLength = textRemoveEvent.getLength();

                                        areaDocument.disableFilter();
                                        area.replaceRange(null, removeEventOffset, removeEventOffset + removeEventLength);
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
