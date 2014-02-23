import javax.swing.*;
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
            this.areaDocument = ((DistributedDocument) area.getDocument());
        else
            this.areaDocument = null;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                if (callback.isDebugging()) Thread.sleep(1000);      // Debugging purposes
                Object obj = inputStream.readObject();


                if (obj instanceof MyConnectionEvent) {
                    handleConnectionEvent((MyConnectionEvent) obj);
                } else if (obj instanceof TextInsertEvent) {
                    handleInsertEvent((TextInsertEvent) obj);
                } else if (obj instanceof TextRemoveEvent) {
                    handleRemoveEvent((TextRemoveEvent) obj);
                }

            } catch (IOException e) {
                e.printStackTrace();
                callback.connectionClosed();
                wasInterrupted = true;
            } catch (Exception e2) {
                e2.printStackTrace();
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

    private void handleRemoveEvent(TextRemoveEvent obj) throws InterruptedException {
        System.out.println("Received a text remove event with offset " + obj.getOffset() + " and length " + obj.getLength() + " and timestamp " + obj.getTimestamp()[0] + "," + obj.getTimestamp()[1]);
        final TextRemoveEvent textRemoveEvent = obj;
        final double[] timestamp = textRemoveEvent.getTimestamp();
        final int senderIndex = textRemoveEvent.getSender();
        while (isNotInCausalOrder(timestamp, senderIndex)) {
            Thread.sleep(100);
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                System.out.println("Started manipulating a remove event with offset " + textRemoveEvent.getOffset() + " and length " + textRemoveEvent.getLength());
                try {
                    if (areaDocument != null) {
                        synchronized (areaDocument) {

                            int receiverIndex = callback.getLamportIndex();
                            ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp[receiverIndex], getCallbackLamportTime(receiverIndex), receiverIndex);
                            LamportTimeComparator comparator = new LamportTimeComparator(receiverIndex);
                            // Sort the events the other client hasn't seen
                            Collections.sort(historyInterval, comparator);
                            boolean ignore = false;
                            // Iterate through the events the other client hasn't seen in order to resolve corrupted events.
                            for (int i = 0; i < historyInterval.size(); i++) {

                                MyTextEvent localEvent = historyInterval.get(i);
                                int removeEventOffset = textRemoveEvent.getOffset();
                                int removeEventTextLengthChange = textRemoveEvent.getTextLengthChange();
                                int removeEventLength = textRemoveEvent.getLength();
                                int localEventOffset = localEvent.getOffset();
                                int localEventTextLengthChange = localEvent.getTextLengthChange();

                                // Check to see if the local event has a lower offset than the received event
                                if (isLocalEventOffsetLower(receiverIndex, removeEventOffset, localEventOffset, senderIndex)) {

                                    // Check to see if our received event is contained inside a local remove event
                                    if (isRemoveContainedInHistoryEvent(receiverIndex, localEvent, removeEventOffset, removeEventLength, localEventOffset, senderIndex)){
                                        // If the received event is contained inside a local remove event, we simply ignore it
                                        ignore = true;

                                    } else if (isEventOffsetOverlapped(receiverIndex, localEvent, removeEventOffset, localEventOffset, senderIndex)) {
                                        // Else, check if the offset of the received remove event is overlapped by a local remove event.
                                        // In this case we subtract the length of the overlapping region from the length of the received event and
                                        // we set the offset of the received event to the offset of the local event.
                                        textRemoveEvent.setLength(removeEventLength - (localEventOffset + ((TextRemoveEvent) localEvent).getLength() - removeEventOffset));
                                        textRemoveEvent.setOffset(localEvent.getOffset());

                                    } else {
                                        // Else the local event must happen at a lower index without overlap, and we simply add/subtract
                                        // the length of the event from the length of the received event.
                                        textRemoveEvent.setOffset(removeEventOffset + localEventTextLengthChange);
                                    }
                                } else if (isLocalEventOffsetLower(0, removeEventOffset + removeEventLength, localEventOffset, 0)) {
                                    // If the offset of the local event isn't lower than the offset of the received event, we check to see if the offset of the local event is
                                    // contained in the received event.
                                    // In this case we either increase the length of the received event by the length of the local event (if it is an insert event)
                                    // or we reduce the length of the received event by the length of the overlapping region (if they are both remove events)
                                    textRemoveEvent.setLength(removeEventLength + Math.max(localEventTextLengthChange, -(removeEventOffset + removeEventLength - localEventOffset)));
                                } else {
                                    // If the local event has a higher offset than and isn't contained by the received event, we simply adjust the offset of the local event accordingly.
                                    localEvent.setOffset(localEventOffset + removeEventTextLengthChange);
                                }
                            }
                            callback.adjustVectorClock(timestamp);
                            int removeEventOffset = textRemoveEvent.getOffset();
                            int removeEventLength = textRemoveEvent.getLength();
                            if (!ignore){
                                areaDocument.disableFilter();
                                area.replaceRange(null, removeEventOffset, removeEventOffset + removeEventLength);
                                areaDocument.enableFilter();
                            }
                        }
                    }
                } catch (IllegalArgumentException ae){
                    System.err.println("Made an illegal remove at offset " + textRemoveEvent.getOffset() + " with length " + textRemoveEvent.getLength() + " and timestamp " + textRemoveEvent.getTimestamp()[0] + "," + textRemoveEvent.getTimestamp()[1] + " in a document with size " + areaDocument.getLength());
                    ae.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }




    private void handleInsertEvent(TextInsertEvent obj) throws InterruptedException {
        System.out.println("Received a text insert event with offset " + obj.getOffset() + " and text " + obj.getText() + " and timestamp " + obj.getTimestamp()[0] + "," + obj.getTimestamp()[1]);
        final TextInsertEvent textInsertEvent = obj;
        final double[] timestamp = textInsertEvent.getTimestamp();
        final int senderIndex = textInsertEvent.getSender();
        while (isNotInCausalOrder(timestamp, senderIndex)) {
            Thread.sleep(100);
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (areaDocument != null) {
                        synchronized (areaDocument) {
                            int receiverIndex = callback.getLamportIndex();
                            ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp[receiverIndex], getCallbackLamportTime(receiverIndex), receiverIndex);
                            LamportTimeComparator comparator = new LamportTimeComparator(receiverIndex);
                            // Sort the events the other client hasn't seen
                            Collections.sort(historyInterval, comparator);
                            boolean ignore = false;
                            // Iterate through the events the other client hasn't seen in order to resolve corrupted events.
                            for (int i = 0; i < historyInterval.size(); i++) {
                                MyTextEvent localEvent = historyInterval.get(i);
                                int insertEventOffset = textInsertEvent.getOffset();
                                int localEventOffset = localEvent.getOffset();
                                int localEventTextLengthChange = localEvent.getTextLengthChange();

                                // Checks if the local event has a lower index than the received insert event
                                if (isLocalEventOffsetLower(receiverIndex, insertEventOffset, localEventOffset, senderIndex)) {

                                    // Checks to see if the event we just received is contained in a local remove event.
                                    if (isInsertedInsideRemove(0, localEvent, insertEventOffset, localEventOffset, 0)) {
                                        // Ignore insert events contained in a local remove event, because the other client will delete the text
                                        // when our remove arrives. The end of the interval is non-inclusive when we decide to ignore.
                                        ignore = true;
                                    } else {
                                        // Else the local event has a lower offset than the received event.
                                        // Just add or subtract the length of the local event from the received event.
                                        textInsertEvent.setOffset(insertEventOffset + localEventTextLengthChange);
                                    }
                                } else {
                                    // Else we must have a local event with a higher offset than the received event.
                                    // Add the length of the received text to the local event in order to keep the history consistent with the text.
                                    localEvent.setOffset(localEventOffset + textInsertEvent.getTextLengthChange());
                                }
                            }
                            callback.adjustVectorClock(timestamp);
                            if (!ignore) {
                                areaDocument.disableFilter();
                                int dotPosBeforeInsert = area.getCaret().getDot();
                                area.insert(textInsertEvent.getText(), textInsertEvent.getOffset());
                                if (textInsertEvent.getOffset() == dotPosBeforeInsert && senderIndex < receiverIndex){
                                    area.getCaret().setDot(dotPosBeforeInsert);
                                }
                                areaDocument.enableFilter();
                            }
                        }
                    }
                } catch (IllegalArgumentException ae){
                    System.err.println("Made an illegal insert at offset " + textInsertEvent.getOffset() + " with text " + textInsertEvent.getText() + " and timestamp " + textInsertEvent.getTimestamp()[0] + "," + textInsertEvent.getTimestamp()[1]);
                    ae.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isInsertedInsideRemove(int priorityIndex, MyTextEvent historyEvent, int insertEventOffset, int historyEventOffset, int yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
               historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= insertEventOffset &&
               (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != insertEventOffset || priorityIndex < yieldingIndex);
    }

    private void handleConnectionEvent(MyConnectionEvent obj) {
        if (obj.getType().equals(ConnectionEventTypes.DISCONNECT_REQUEST)) {
            callback.replyToDisconnect();
        } else if (obj.getType().equals(ConnectionEventTypes.DISCONNECT_REPLY_OK)) {
            callback.connectionClosed();
        }
    }

    private boolean isNotInCausalOrder(double[] timestamp, int senderIndex) {
        return timestamp[senderIndex] != getCallbackLamportTime(senderIndex) + 1 || timestamp[callback.getLamportIndex()] > getCallbackLamportTime(callback.getLamportIndex());
    }

    private boolean isLocalEventOffsetLower(int priorityIndex, int textEventOffset, int historyEventOffset, int yieldingIndex) {
        return historyEventOffset <= textEventOffset && (historyEventOffset != textEventOffset || priorityIndex < yieldingIndex);
    }

    private double getCallbackLamportTime(int senderIndex) {
        return callback.getLamportTime(senderIndex);
    }

    private boolean isEventOffsetOverlapped(int priorityIndex, MyTextEvent historyEvent, int removeEventOffset, int historyEventOffset, int yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
               removeEventOffset <= historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() &&
               (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset || priorityIndex < yieldingIndex);
    }

    private boolean isRemoveContainedInHistoryEvent(int priorityIndex, MyTextEvent historyEvent, int removeEventOffset, int removeEventLength, int historyEventOffset, int yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
                historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= removeEventOffset + removeEventLength &&
                (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset + removeEventLength || priorityIndex < yieldingIndex);
    }

}
