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

    private boolean wasInterrupted = false;
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
        while (!wasInterrupted) {
            try {

                Object obj = inputStream.readObject();

                if (callback.isDebugging()) Thread.sleep(1000);      // Debugging purposes

                if (obj instanceof MyConnectionEvent) {
                    handleConnectionEvent((MyConnectionEvent) obj);
                } else if (obj instanceof TextInsertEvent && !callback.eventHasBeenReceived((MyTextEvent) obj)) {
                    handleInsertEvent((TextInsertEvent) obj);
                    callback.addEventToReceived((MyTextEvent) obj);
                } else if (obj instanceof TextRemoveEvent && !callback.eventHasBeenReceived((MyTextEvent) obj)) {
                    callback.addEventToReceived((MyTextEvent) obj);
                    handleRemoveEvent((TextRemoveEvent) obj);
                }

            } catch (IOException e) {
                e.printStackTrace();
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
                callback.incrementReplayThreadCounter();
                boolean myOwnEvent = false;
                try {
                    while (isNotInCausalOrder(timestamp, senderIndex) && !myOwnEvent) {
                        if(senderIndex.equals(callback.getLamportIndex())) {
                            myOwnEvent = true;
                        }
                        if(!myOwnEvent) Thread.yield();

                    }
                    if (areaDocument != null && !myOwnEvent) {
                        synchronized (areaDocument) {

                            String receiverIndex = callback.getLamportIndex();
                            ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(textRemoveEvent);
                            LamportTimeComparator comparator = new LamportTimeComparator(senderIndex);
                            // Sort the events the other client hasn't seen
                            Collections.sort(historyInterval, comparator);
                            boolean ignore = false;
                            int removeEventPlacementOffset = textRemoveEvent.getOffset();
                            int removeEventPlacementLength = textRemoveEvent.getLength();
                            MyTextEvent lastEvent = historyInterval.get(0);
                            int oldRemoveEventOffset = textRemoveEvent.getOffset();
                            int oldRemoveEventLength = textRemoveEvent.getLength();
                            int removeOffsetAdjust = 0;
                            int removeLengthAdjust = 0;
                            // Iterate through the events the other client hasn't seen in order to resolve corrupted events.
                            for (int i = 0; i < historyInterval.size(); i++) {

                                MyTextEvent localEvent = historyInterval.get(i);

                                //int removeEventTextLengthChange = textRemoveEvent.getTextLengthChange();

                                int removeEventOffset = lastEvent.getSender().equals(localEvent.getSender()) && lastEvent.getOffset() <= localEvent.getOffset() ? oldRemoveEventOffset : textRemoveEvent.getOffset();

                                int removeEventLength = lastEvent.getSender().equals(localEvent.getSender()) && lastEvent.getOffset() <= localEvent.getOffset() ? oldRemoveEventLength : textRemoveEvent.getLength();

                                int localEventOffset = localEvent.getOffset();
                                int localEventTextLengthChange = localEvent.getTextLengthChange();
                                String localEventIndex = localEvent.getSender();

                                System.out.println("Started manipulating a remove event with offset " + textRemoveEvent.getOffset() + " and length " + textRemoveEvent.getLength() + " by comparing it to the event " + (localEvent instanceof TextInsertEvent ? " inserting " + ((TextInsertEvent) localEvent).getText() + " at " + localEvent.getOffset() : " removing from " + localEvent.getOffset() + " to " + (localEvent.getOffset()-localEvent.getTextLengthChange())));
                                // Check to see if the local event has a lower offset than the received event
                                if (isLocalEventOffsetLower(localEventIndex, removeEventOffset, localEventOffset, senderIndex)) {

                                    // Check to see if our received event is contained inside a local remove event
                                    if (isRemoveContainedInHistoryEvent(localEventIndex, localEvent, removeEventOffset, removeEventLength, localEventOffset, senderIndex)){
                                        // If the received event is contained inside a local remove event, we simply ignore it and adjust the length of the local remove event.
                                        ignore = true;
                                        textRemoveEvent.setIgnored(true);
                                        //((TextRemoveEvent)localEvent).setLength(((TextRemoveEvent) localEvent).getLength() + textRemoveEvent.getTextLengthChange());
                                    } else if (isEventOffsetOverlapped(localEventIndex, localEvent, removeEventOffset, localEventOffset, senderIndex)) {
                                        // Else, check if the offset of the received remove event is overlapped by a local remove event.
                                        // In this case we subtract the length of the overlapping region from the length of the received event and
                                        // we set the offset of the received event to the offset of the local event.
                                        if (!localEvent.isIgnored()){
                                            removeEventPlacementLength = removeEventPlacementLength - (localEventOffset + ((TextRemoveEvent) localEvent).getLength() - removeEventOffset);
                                            removeEventPlacementOffset = localEvent.getOffset();
                                        }


                                    } else {
                                        // Else the local event must happen at a lower index without overlap, and we simply add/subtract
                                        // the length of the event from the length of the received event.
                                        /*if ((localEventIndex.equals(senderIndex) && localEvent.getTimestamp().get(localEvent.getSender()) > textRemoveEvent.getTimestamp().get(textRemoveEvent.getSender()))) {

                                            textRemoveEvent.setOffset(removeEventOffset + localEventTextLengthChange);
                                        }
                                        removeEventPlacementOffset += localEventTextLengthChange;
                                                                                                     */
                                        if ((localEventIndex.equals(senderIndex) && localEvent.getTimestamp().get(localEvent.getSender()) < textRemoveEvent.getTimestamp().get(senderIndex))){
                                            textRemoveEvent.setOffset(removeEventOffset - localEventTextLengthChange);
                                            removeEventOffset -= localEventTextLengthChange;
                                            removeOffsetAdjust -= localEventTextLengthChange;
                                            System.out.println("Modified the offset of the event removing by " + textRemoveEvent.getLength() + " by moving it by " + (-localEventTextLengthChange));
                                        } else if (!localEventIndex.equals(senderIndex) && !localEvent.isIgnored()){
                                            //textInsertEvent.setOffset(textInsertEvent.getOffset() + localEventTextLengthChange);
                                            removeEventOffset +=localEventTextLengthChange;
                                            //insertOffsetAdjust += localEventTextLengthChange;
                                            removeEventPlacementOffset += localEventTextLengthChange;
                                            System.out.println("Modified the offset of the event removing by " + textRemoveEvent.getLength() + " by moving it by " + localEventTextLengthChange);
                                            System.out.println("Placement offset is now " + removeEventPlacementOffset);
                                        }

                                    }
                                } else if (isLocalEventOffsetLower("", removeEventOffset + removeEventLength, localEventOffset, "")) {
                                    // If the offset of the local event isn't lower than the offset of the received event, we check to see if the offset of the local event is
                                    // contained in the received event.
                                    // In this case we either increase the length of the received event by the length of the local event (if it is an insert event)
                                    // or we reduce the length of the received event by the length of the overlapping region (if they are both remove events)
                                    //if (!(localEventIndex.equals(senderIndex) && localEvent.getTimestamp().get(localEvent.getSender()) < textRemoveEvent.getTimestamp().get(textRemoveEvent.getSender()))){
                                        //
                                        System.out.println("Modified the event removing from " + textRemoveEvent.getOffset() + " to " + (textRemoveEvent.getOffset()+textRemoveEvent.getLength()) + " by adjusting the length by " + Math.max(localEventTextLengthChange, -(removeEventOffset + removeEventLength - localEventOffset)));
                                    //}
                                    if ((localEventIndex.equals(senderIndex) && localEvent.getTimestamp().get(localEvent.getSender()) < textRemoveEvent.getTimestamp().get(senderIndex))){

                                        textRemoveEvent.setLength(removeEventLength - Math.max(localEventTextLengthChange, -(removeEventOffset + removeEventLength - localEventOffset)));
                                        removeEventLength -= Math.max(localEventTextLengthChange, -(removeEventPlacementOffset + removeEventPlacementLength - localEventOffset));
                                        removeLengthAdjust -= Math.max(localEventTextLengthChange, -(removeEventPlacementOffset + removeEventPlacementLength - localEventOffset));
                                    } else if (!localEventIndex.equals(senderIndex) && !localEvent.isIgnored()){
                                        if ( localEventOffset - localEventTextLengthChange < removeEventOffset + removeEventLength){
                                            localEvent.setIgnored(true);
                                        }
                                        removeEventLength += Math.max(localEventTextLengthChange, -(removeEventPlacementOffset + removeEventPlacementLength - localEventOffset));
                                        removeEventPlacementLength += Math.max(localEventTextLengthChange, -(removeEventPlacementOffset + removeEventPlacementLength - localEventOffset));
                                    }

                                } else {
                                    // If the local event has a higher offset than and isn't contained by the received event, we simply adjust the offset of the local event accordingly.
                                    if (localEvent.getSender().equals(textRemoveEvent.getSender()) && localEvent.getOffset() < textRemoveEvent.getOffset()){
                                        localEvent.setOffset(localEventOffset - removeEventLength);
                                    }
                                }
                                oldRemoveEventOffset = removeEventOffset;
                                oldRemoveEventLength = removeEventLength;
                            }
                            textRemoveEvent.setOffset(textRemoveEvent.getOffset()-removeOffsetAdjust);
                            textRemoveEvent.setLength(textRemoveEvent.getLength()-removeLengthAdjust);
                            callback.adjustVectorClock(timestamp);
                            if (!ignore){
                                areaDocument.disableFilter();
                                System.out.println("Going to remove from " + removeEventPlacementOffset + " to " + removeEventPlacementLength);
                                area.replaceRange(null, removeEventPlacementOffset, removeEventPlacementOffset + removeEventPlacementLength);
                                System.out.println("Text is now " + area.getText());
                                areaDocument.enableFilter();
                            }
                            callback.addEventToHistory(textRemoveEvent);
                            callback.forwardEvent(textRemoveEvent);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
                callback.decrementReplayThreadCounter();
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
                callback.incrementReplayThreadCounter();
                boolean myOwnEvent = false;
                try {
                    while (isNotInCausalOrder(timestamp, senderIndex) && !myOwnEvent) {
                        if(senderIndex.equals(callback.getLamportIndex())) {
                            myOwnEvent = true;
                        }
                        if(!myOwnEvent) Thread.yield();


                    }
                    if (areaDocument != null && !myOwnEvent) {
                        synchronized (areaDocument) {
                            String receiverIndex = callback.getLamportIndex();
                            ArrayList<MyTextEvent> historyInterval = callback.getEventHistoryInterval(textInsertEvent);
                            LamportTimeComparator comparator = new LamportTimeComparator(senderIndex);
                            // Sort the events the other client hasn't seen
                            Collections.sort(historyInterval, comparator);
                            boolean ignore = false;
                            int insertEventPlacementOffset = textInsertEvent.getOffset();
                            // TextRemoveEvent ignoreRemoveEvent = null;
                            // Iterate through the events the other client hasn't seen in order to resolve corrupted events.
                            MyTextEvent lastEvent =historyInterval.get(0);
                            int oldInsertEventOffset = textInsertEvent.getOffset();
                            int insertOffsetAdjust = 0;
                            for (int i = 0; i < historyInterval.size(); i++) {
                                MyTextEvent localEvent = historyInterval.get(i);
                                // int insideRemoveEventOffsetAdjust = (ignoreRemoveEvent != null && !localEvent.isIgnored() ? ignoreRemoveEvent.getLength() : 0 );
                                int insertEventOffset = lastEvent.getSender().equals(localEvent.getSender()) && lastEvent.getOffset() <= localEvent.getOffset() ? oldInsertEventOffset : textInsertEvent.getOffset();

                                int localEventOffset = localEvent.getOffset();// + insideRemoveEventOffsetAdjust;
                                int localEventTextLengthChange = localEvent.getTextLengthChange();
                                String localEventIndex = localEvent.getSender();
                                lastEvent = localEvent;
                                System.out.println("Compared the event inserting " + textInsertEvent.getText() + " at offset " + insertEventOffset + " to the event " + (localEvent instanceof TextInsertEvent ? "inserting " + ((TextInsertEvent) localEvent).getText() + " at offset " + localEventOffset : " removing from offset " + localEventOffset));
                                // Checks if the local event has a lower index than the received insert event
                                if (isLocalEventOffsetLower(localEventIndex, insertEventOffset, localEventOffset, senderIndex)) {

                                    // Checks to see if the event we just received is contained in a local remove event.
                                    if (isInsertedInsideRemove(localEvent, insertEventOffset, localEventOffset)) {
                                        // Ignore insert events contained in a local remove event, because the other client will delete the text
                                        // when our remove arrives. The end of the interval is non-inclusive when we decide to ignore.
                                        // Also adjust the length of the remove event in order to ensure consistency with the other
                                        // client's event history.
                                        System.out.println("Event was ignored");
                                        ignore = true;
                                        textInsertEvent.setIgnored(true);
                                        /*
                                        if (ignoreRemoveEvent == null || ignoreRemoveEvent.getOffset()+ignoreRemoveEvent.getLength() < localEvent.getOffset() + ((TextRemoveEvent)localEvent).getLength()) {
                                            ignoreRemoveEvent = ((TextRemoveEvent)localEvent);
                                        }    */
                                        //((TextRemoveEvent)localEvent).setLength(((TextRemoveEvent) localEvent).getLength() + textInsertEvent.getTextLengthChange());
                                    } else {
                                        // Else the local event has a lower offset than the received event.
                                        // Just add or subtract the length of the local event from the received event.
                                        if ((localEventIndex.equals(senderIndex) && localEvent.getTimestamp().get(localEvent.getSender()) < textInsertEvent.getTimestamp().get(senderIndex))){
                                            textInsertEvent.setOffset(insertEventOffset - localEventTextLengthChange);
                                            insertEventOffset -= localEventTextLengthChange;
                                            insertOffsetAdjust -= localEventTextLengthChange;
                                            //insertEventPlacementOffset -= localEventTextLengthChange;
                                            System.out.println("Modified the offset of the event inserting " + textInsertEvent.getText() + " by moving it by " + (-localEventTextLengthChange));
                                        } else if (!localEventIndex.equals(senderIndex) && !localEvent.isIgnored()){
                                            //textInsertEvent.setOffset(textInsertEvent.getOffset() + localEventTextLengthChange);
                                            insertEventOffset +=localEventTextLengthChange;
                                            //insertOffsetAdjust += localEventTextLengthChange;
                                            insertEventPlacementOffset += localEventTextLengthChange;
                                            System.out.println("Modified the offset of the event inserting " + textInsertEvent.getText() + " by moving it by " + localEventTextLengthChange);

                                        }
                                        System.out.println("Placement index is now " + insertEventPlacementOffset);


                                        //}
                                        /*if (ignoreRemoveEvent!= null && ignoreRemoveEvent.getOffset()+ignoreRemoveEvent.getLength()-textInsertEvent.getTextLengthChange() <= textInsertEvent.getOffset()){
                                            //System.out.println("Event was pushed out of the remove event and is no longer ignored");
                                            ignore = false;
                                            ignoreRemoveEvent.setLength(ignoreRemoveEvent.getLength()-textInsertEvent.getTextLengthChange());
                                            textInsertEvent.setOffset(textInsertEvent.getOffset() - ignoreRemoveEvent.getLength());
                                            textInsertEvent.setIgnored(false);
                                            ignoreRemoveEvent = null;
                                        }*/
                                    }
                                } else {
                                    // Else we must have a local event with a higher offset than the received event.
                                    // Add the length of the received text to the local event in order to keep the history consistent with the text.
                                    if (localEvent.getSender().equals(textInsertEvent.getSender()) && localEvent.getOffset() > textInsertEvent.getOffset()){
                                        localEvent.setOffset(localEventOffset + textInsertEvent.getTextLengthChange());
                                    }
                                }
                                oldInsertEventOffset = insertEventOffset;

                            }
                            textInsertEvent.setOffset(textInsertEvent.getOffset() - insertOffsetAdjust);
                            callback.adjustVectorClock(timestamp);
                            if (!ignore) {
                                areaDocument.disableFilter();
                                int dotPosBeforeInsert = area.getCaret().getDot();
                                //area.insert(textInsertEvent.getText(), textInsertEvent.getOffset());
                                area.insert(textInsertEvent.getText(),insertEventPlacementOffset);
                                // If both clients are writing to the same offset they push each others' carets in front of the text creating interlaced text.
                                // In order to avoid this, the client with the highest index yields the position in front and moves his caret one letter back,
                                // thereby avoiding scrambled text.
                                if (textInsertEvent.getOffset() == dotPosBeforeInsert && senderIndex.compareTo(receiverIndex) < 0){
                                    area.getCaret().setDot(dotPosBeforeInsert);
                                }
                                System.out.println("Text is now " + area.getText());
                                areaDocument.enableFilter();
                            }
                            callback.addEventToHistory(textInsertEvent);
                            callback.forwardEvent(textInsertEvent);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                callback.decrementReplayThreadCounter();
            }
        }).start();
    }

    private boolean isInsertedInsideRemove(MyTextEvent historyEvent, int insertEventOffset, int historyEventOffset) {
        return historyEvent instanceof TextRemoveEvent &&
               historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() > insertEventOffset;
    }

    private void handleConnectionEvent(MyConnectionEvent obj) throws IOException {
        if (obj.getType().equals(ConnectionEventTypes.SCRAMBLE_EVENT)) {
            callback.addToClock(((ScrambleEvent) obj).getAddedClocks());
            if(!((ScrambleEvent) obj).getDeadAddress().equals("no_dead_address")) {
                callback.connectionClosed(((ScrambleEvent) obj).getDeadAddress());
            }
            System.out.println("Received scramble event, starting SCRAMBLE");
            callback.scrambleNetwork(((ScrambleEvent)obj));
            inputStream.close();
            wasInterrupted = true;
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
