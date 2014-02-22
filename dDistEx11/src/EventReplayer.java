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
                System.out.println("Object received");
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
        final TextRemoveEvent textRemoveEvent = obj;
        final double[] timestamp = textRemoveEvent.getTimestamp();
        final int senderIndex = textRemoveEvent.getSender();
        while (isInCausalOrder(timestamp, senderIndex)) {
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
                            Collections.sort(historyInterval, comparator);
                            boolean ignore = false;
                            for (MyTextEvent historyEvent : historyInterval) {
                                int removeEventOffset = textRemoveEvent.getOffset();
                                int removeEventTextLengthChange = textRemoveEvent.getTextLengthChange();
                                int removeEventLength = textRemoveEvent.getLength();
                                int historyEventOffset = historyEvent.getOffset();
                                int historyEventTextLengthChange = historyEvent.getTextLengthChange();

                                if (isHistoryEventOffsetLower(receiverIndex, removeEventOffset, historyEventOffset, senderIndex)) {
                                    if (isRemoveContainedInHistoryEvent(receiverIndex, historyEvent, removeEventOffset, removeEventLength, historyEventOffset, senderIndex)){
                                        ignore = true;
                                    } else if (isEventOffsetOverlapped(receiverIndex, historyEvent, removeEventOffset, historyEventOffset, senderIndex)) {
                                        textRemoveEvent.setLength(removeEventLength - (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() - removeEventOffset));
                                        textRemoveEvent.setOffset(historyEvent.getOffset());
                                    } else {
                                        textRemoveEvent.setOffset(removeEventOffset + historyEventTextLengthChange);
                                    }
                                } else if (isHistoryEventOffsetLower(receiverIndex, removeEventOffset + removeEventLength, historyEventOffset, senderIndex)) {
                                    textRemoveEvent.setLength(removeEventLength + Math.max(historyEventTextLengthChange, -(removeEventOffset + removeEventLength - historyEventOffset)));
                                } else {
                                    historyEvent.setOffset(historyEventOffset + removeEventTextLengthChange);
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
                    System.err.println("Made an illegal remove at offset " + textRemoveEvent.getOffset() + " with length " + textRemoveEvent.getLength() + " in a document with size " + areaDocument.getLength());
                    ae.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isRemoveContainedInHistoryEvent(int receiverIndex, MyTextEvent historyEvent, int removeEventOffset, int removeEventLength, int historyEventOffset, int senderIndex) {
        return historyEvent instanceof TextRemoveEvent &&
                historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= removeEventOffset + removeEventLength &&
                (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset + removeEventLength || receiverIndex < senderIndex);
    }


    private void handleInsertEvent(TextInsertEvent obj) throws InterruptedException {
        final TextInsertEvent textInsertEvent = obj;
        final double[] timestamp = textInsertEvent.getTimestamp();
        final int senderIndex = textInsertEvent.getSender();
        while (isInCausalOrder(timestamp, senderIndex)) {
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
                            Collections.sort(historyInterval, comparator);
                            boolean ignore = false;
                            for (MyTextEvent historyEvent : historyInterval) {
                                int insertEventOffset = textInsertEvent.getOffset();
                                int historyEventOffset = historyEvent.getOffset();
                                int historyEventTextLengthChange = historyEvent.getTextLengthChange();
                                if (isHistoryEventOffsetLower(receiverIndex, insertEventOffset, historyEventOffset, senderIndex)) {
                                    if (isInsertedInsideRemove(receiverIndex, historyEvent, insertEventOffset, historyEventOffset, senderIndex)) {
                                        ignore = true;
                                    } else {
                                        textInsertEvent.setOffset(insertEventOffset + historyEventTextLengthChange);
                                    }
                                } else {
                                    historyEvent.setOffset(historyEventOffset + textInsertEvent.getTextLengthChange());
                                }
                            }
                            callback.adjustVectorClock(timestamp);
                            if (!ignore) {
                                areaDocument.disableFilter();
                                area.insert(textInsertEvent.getText(), textInsertEvent.getOffset());
                                areaDocument.enableFilter();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isInsertedInsideRemove(int receiverIndex, MyTextEvent historyEvent, int insertEventOffset, int historyEventOffset, int senderIndex) {
        return historyEvent instanceof TextRemoveEvent &&
               historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= insertEventOffset &&
               (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != insertEventOffset || receiverIndex < senderIndex);
    }

    private void handleConnectionEvent(MyConnectionEvent obj) {
        if (obj.getType().equals(ConnectionEventTypes.DISCONNECT_REQUEST)) {
            callback.replyToDisconnect();
        } else if (obj.getType().equals(ConnectionEventTypes.DISCONNECT_REPLY_OK)) {
            callback.connectionClosed();
        }
    }

    private boolean isInCausalOrder(double[] timestamp, int senderIndex) {
        return timestamp[senderIndex] != getCallbackLamportTime(senderIndex) + 1 || timestamp[callback.getLamportIndex()] > getCallbackLamportTime(callback.getLamportIndex());
    }

    private boolean isHistoryEventOffsetLower(int receiverIndex, int textEventOffset, int historyEventOffset, int senderIndex) {
        return historyEventOffset <= textEventOffset && (historyEventOffset != textEventOffset || receiverIndex < senderIndex);
    }

    private double getCallbackLamportTime(int senderIndex) {
        return callback.getLamportTime(senderIndex);
    }

    private boolean isEventOffsetOverlapped(int receiverIndex, MyTextEvent historyEvent, int removeEventOffset, int historyEventOffset, int senderIndex) {
        return historyEvent instanceof TextRemoveEvent &&
               removeEventOffset <= historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() &&
               (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset || receiverIndex < senderIndex);
    }

}
