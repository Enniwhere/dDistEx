import java.util.HashMap;
import java.util.Map;

/*
 *  Helper class comparing events, to avoid bloating the EventReplayer.
 */
public class EventComparisonHelper {
    private final DistributedTextEditor callback;

    public EventComparisonHelper(DistributedTextEditor distributedTextEditor) {
        this.callback = distributedTextEditor;
    }

    boolean isInsertedInsideRemove(MyTextEvent historyEvent, int insertEventOffset, int historyEventOffset) {
        return historyEvent instanceof TextRemoveEvent &&
                historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() > insertEventOffset;
    }

    boolean isNotInCausalOrder(Map<String, Integer> timestamp, String senderIndex) {
        boolean result = timestamp.get(senderIndex) != getCallbackLamportTime(senderIndex) + 1;
        HashMap<String, Integer> tempTimestamp = new HashMap<String, Integer>(timestamp);
        tempTimestamp.remove(senderIndex);
        for (String id : tempTimestamp.keySet()) {
            result = result || timestamp.get(id) > getCallbackLamportTime(id);
        }
        return result;
    }

    boolean isLocalEventOffsetLower(String priorityIndex, int textEventOffset, int historyEventOffset, String yieldingIndex) {
        return historyEventOffset <= textEventOffset && (historyEventOffset != textEventOffset || priorityIndex.compareTo(yieldingIndex) < 0);
    }

    int getCallbackLamportTime(String senderIndex) {
        return callback.getLamportTime(senderIndex);
    }

    boolean isEventOffsetOverlapped(String priorityIndex, MyTextEvent historyEvent, int removeEventOffset, int historyEventOffset, String yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
                removeEventOffset <= historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() &&
                (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset || priorityIndex.compareTo(yieldingIndex) < 0);
    }

    boolean isRemoveContainedInHistoryEvent(String priorityIndex, MyTextEvent historyEvent, int removeEventOffset, int removeEventLength, int historyEventOffset, String yieldingIndex) {
        return historyEvent instanceof TextRemoveEvent &&
                historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() >= removeEventOffset + removeEventLength &&
                (historyEventOffset + ((TextRemoveEvent) historyEvent).getLength() != removeEventOffset + removeEventLength || priorityIndex.compareTo(yieldingIndex) < 0);
    }
}