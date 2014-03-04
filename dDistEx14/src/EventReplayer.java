import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * This eventReplayer uses events from its inputStream and replays them in the textfield in the editor
 */
public class EventReplayer implements Runnable {


    private final String address;
    private ObjectInput inputStream;
    private final JTextArea area;
    private DistributedTextEditor callback;
    private DistributedDocument areaDocument;

    public EventReplayer(ObjectInput inputStream, final JTextArea area, DistributedTextEditor callback, String address) {
        this.inputStream = inputStream;
        this.area = area;
        this.callback = callback;
        this.address = address;
        if (area.getDocument() instanceof DistributedDocument)
            this.areaDocument = ((DistributedDocument) area.getDocument());
        else
            this.areaDocument = null;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {

                Object obj = inputStream.readObject();

                if (callback.isDebugging()) Thread.sleep(1000);      // Debugging purposes

                if (obj instanceof MyConnectionEvent) {
                    handleConnectionEvent((MyConnectionEvent) obj);
                } else if (obj instanceof TextInsertEvent) {
                    handleInsertEvent((TextInsertEvent) obj);
                } else if (obj instanceof TextRemoveEvent) {
                    handleRemoveEvent((TextRemoveEvent) obj);
                } else System.out.println("What happen?");

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
        final TextRemoveEvent textRemoveEvent = obj;
        final Map<String, Integer> timestamp = textRemoveEvent.getTimestamp();
        final String senderIndex = textRemoveEvent.getSender();

        new Thread(new Runnable() {
            public void run() {

                System.out.println("Started manipulating a remove event with offset " + textRemoveEvent.getOffset() + " and length " + textRemoveEvent.getLength());
                try {
                    while (isNotInCausalOrder(timestamp, senderIndex)) {
                        Thread.sleep(10);
                    }
                    if (areaDocument != null) {
                        synchronized (areaDocument) {

                            String receiverIndex = callback.getLamportIndex();
                            ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp);
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
                                String localEventIndex = localEvent.getSender();
                                // Check to see if the local event has a lower offset than the received event
                                if (isLocalEventOffsetLower(localEventIndex, removeEventOffset, localEventOffset, senderIndex)) {

                                    // Check to see if our received event is contained inside a local remove event
                                    if (isRemoveContainedInHistoryEvent(localEventIndex, localEvent, removeEventOffset, removeEventLength, localEventOffset, senderIndex)){
                                        // If the received event is contained inside a local remove event, we simply ignore it and adjust the length of the local remove event.
                                        ignore = true;
                                        ((TextRemoveEvent)localEvent).setLength(((TextRemoveEvent) localEvent).getLength() + textRemoveEvent.getTextLengthChange());
                                    } else if (isEventOffsetOverlapped(localEventIndex, localEvent, removeEventOffset, localEventOffset, senderIndex)) {
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
                                } else if (isLocalEventOffsetLower("", removeEventOffset + removeEventLength, localEventOffset, "")) {
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
                            callback.addEventToHistory(textRemoveEvent);
                        }
                    }
                } catch (IllegalArgumentException ae){
                    ae.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //.
    private void handleInsertEvent(TextInsertEvent obj) throws InterruptedException {
        final TextInsertEvent textInsertEvent = obj;
        final Map<String, Integer> timestamp = textInsertEvent.getTimestamp();
        final String senderIndex = textInsertEvent.getSender();

        new Thread(new Runnable() {
            public void run() {
                try {
                    while (isNotInCausalOrder(timestamp, senderIndex)) {
                        Thread.sleep(10);
                    }
                    if (areaDocument != null) {
                        synchronized (areaDocument) {
                            String receiverIndex = callback.getLamportIndex();
                            ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(timestamp);
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
                                String localEventIndex = localEvent.getSender();
                                System.out.println("Compared the event inserting " + textInsertEvent.getText() + " at offset " + textInsertEvent.getOffset() + " to the event " + (localEvent instanceof TextInsertEvent ? "inserting " + ((TextInsertEvent) localEvent).getText() + " at offset " + localEvent.getOffset() : " removing from offset " + localEvent.getOffset()));
                                // Checks if the local event has a lower index than the received insert event
                                if (isLocalEventOffsetLower(localEventIndex, insertEventOffset, localEventOffset, senderIndex)) {

                                    // Checks to see if the event we just received is contained in a local remove event.
                                    if (isInsertedInsideRemove(localEvent, insertEventOffset, localEventOffset)) {
                                        // Ignore insert events contained in a local remove event, because the other client will delete the text
                                        // when our remove arrives. The end of the interval is non-inclusive when we decide to ignore.
                                        // Also adjust the length of the remove event in order to ensure consistency with the other
                                        // client's event history.
                                        ignore = true;
                                        ((TextRemoveEvent)localEvent).setLength(((TextRemoveEvent) localEvent).getLength() + textInsertEvent.getTextLengthChange());
                                    } else {
                                        // Else the local event has a lower offset than the received event.
                                        // Just add or subtract the length of the local event from the received event.
                                        textInsertEvent.setOffset(insertEventOffset + localEventTextLengthChange);
                                        System.out.println("Modified the offset of the event inserting " + textInsertEvent.getText() + " by moving it by " + localEventTextLengthChange);
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
                                // If both clients are writing to the same offset they push each others' carets in front of the text creating interlaced text.
                                // In order to avoid this, the client with the highest index yields the position in front and moves his caret one letter back,
                                // thereby avoiding scrambled text.
                                if (textInsertEvent.getOffset() == dotPosBeforeInsert && senderIndex.compareTo(receiverIndex) < 0){
                                    area.getCaret().setDot(dotPosBeforeInsert);
                                }

                                areaDocument.enableFilter();
                            }
                            callback.addEventToHistory(textInsertEvent);
                        }
                    }
                } catch (IllegalArgumentException ae){
                    ae.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean isInsertedInsideRemove(MyTextEvent historyEvent, int insertEventOffset, int historyEventOffset) {
        return historyEvent instanceof TextRemoveEvent &&
               historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() > insertEventOffset;
    }

    private void handleConnectionEvent(MyConnectionEvent obj) {
        if (obj.getType().equals(ConnectionEventTypes.DISCONNECT_REQUEST)) {
            callback.replyToDisconnect(address);
        } else if (obj.getType().equals(ConnectionEventTypes.DISCONNECT_REPLY_OK)) {
            callback.connectionClosed();
        } else if (obj.getType().equals(ConnectionEventTypes.SCRAMBLE_EVENT)) {
            callback.addToClock(((ScrambleEvent) obj).getAddedClocks());
            callback.scrambleNetwork(((ScrambleEvent)obj));
        }
    }

    private boolean isNotInCausalOrder(Map<String, Integer> timestamp, String senderIndex) {
        boolean result = timestamp.get(senderIndex) != getCallbackLamportTime(senderIndex) + 1;
        HashMap<String, Integer> tempTimestamp = new HashMap<String, Integer>(timestamp);
        tempTimestamp.remove(senderIndex);
        for (String id : tempTimestamp.keySet()){
            result = result || timestamp.get(id) > getCallbackLamportTime(id);
        }
        return result;
    }

    private boolean isLocalEventOffsetLower(String priorityIndex, int textEventOffset, int historyEventOffset, String yieldingIndex) {
        return historyEventOffset <= textEventOffset && (historyEventOffset != textEventOffset || priorityIndex.compareTo(yieldingIndex) < 0);
    }

    private int getCallbackLamportTime(String senderIndex) {
        return callback.getLamportTime(senderIndex);
    }

    private boolean isEventOffsetOverlapped(String priorityIndex, MyTextEvent historyEvent, int removeEventOffset, int historyEventOffset, String yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
               removeEventOffset <= historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() &&
               (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset || priorityIndex.compareTo(yieldingIndex) < 0);
    }

    private boolean isRemoveContainedInHistoryEvent(String priorityIndex, MyTextEvent historyEvent, int removeEventOffset, int removeEventLength, int historyEventOffset, String yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
                historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= removeEventOffset + removeEventLength &&
                (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset + removeEventLength || priorityIndex.compareTo(yieldingIndex) < 0);
    }

}
