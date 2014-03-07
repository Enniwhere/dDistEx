
import java.util.*;

/**
 * Created by simon on 2/26/14.
 */
public class DistributedTextEditorStub implements DistributedTextEditor {

    private final Map<String, Integer> vectorClockMap;
    private final String lamportIndex;
    private ArrayList<MyTextEvent> eventHistory = new ArrayList<MyTextEvent>();
    private boolean debugIsOn = false;

    public DistributedTextEditorStub(Map<String, Integer> clock, String index){
        vectorClockMap = clock;
        lamportIndex = index;
    }

    public void connectionClosed() {
        System.out.println("Tried to close the replayer thread");
    }

    @Override
    public void connectionClosed(String index) {

    }

    @Override
    public int getPortNumberTextField() {
        return 0;  //This method was auto-implemented
    }

    @Override
    public String getIPAddress() {
        return null;  //This method was auto-implemented
    }

    @Override
    public void replyToDisconnect(String eventReplayer) {
        //This method was auto-implemented
    }

    @Override
    public int getLamportTime(String index) {
        return vectorClockMap.get(index);
    }

    @Override
    public String getLamportIndex() {
        return lamportIndex;
    }

    @Override
    public void incrementLamportTime() {
        synchronized (vectorClockMap){
            vectorClockMap.put(lamportIndex, getLamportTime(lamportIndex) + 1);
        }
    }

    @Override
    public Map<String, Integer> getTimestamp() {
        return new HashMap<String, Integer>(vectorClockMap);
    }

    @Override
    public void adjustVectorClock(Map<String, Integer> hashMap) {
        synchronized (vectorClockMap){
            for (String s : hashMap.keySet()) {
                vectorClockMap.put(s, Math.max(vectorClockMap.get(s), hashMap.get(s)));
            }
        }
    }

    public ArrayList<MyTextEvent> getEventHistoryInterval(MyTextEvent textEvent) {
        ArrayList<MyTextEvent> res = new ArrayList<MyTextEvent>();
        Map<String,Integer> timestamp = textEvent.getTimestamp();
        ArrayList<String> sortedKeys = new ArrayList<String>(timestamp.keySet());
        Collections.sort(sortedKeys, new StringDescendingComparator());
        synchronized (eventHistory) {
            for (MyTextEvent historyEvent : eventHistory) {
                if (historyEvent.getTimestamp().get(historyEvent.getSender()) > timestamp.get(historyEvent.getSender())){
                    res.add(historyEvent);
                } else if (historyEvent.getSender().equals(textEvent.getSender()) &&
                           historyEvent.getTimestamp().get(historyEvent.getSender()) < timestamp.get(historyEvent.getSender())){
                    res.add(historyEvent);
                }
            }
        }
        return res;
    }

    @Override
    public void addEventToHistory(MyTextEvent textEvent) {
        synchronized (eventHistory) {
            eventHistory.add(textEvent);
        }
    }

    @Override
    public boolean isDebugging() {
        return debugIsOn;
    }
        //TODO: IMPLEMENT THIS

    @Override
    public void addToClock(Map<String, Integer> map) {
        //TODO: IMPLEMENT THIS
    }

    @Override
    public void handleSetupConnection(SetupConnectionEvent setupConnectionEvent) {
        //TODO: IMPLEMENTS THIS
    }

    @Override
    public void scrambleNetwork(ScrambleEvent scrambleEvent) {
        //This method was auto-implemented
    }

    @Override
    public void incrementReplayThreadCounter() {

    }

    @Override
    public void decrementReplayThreadCounter() {

    }

    @Override
    public int getScrambleLamportClock() {
        return 0;  //This method was auto-implemented
    }

    @Override
    public Map<String, Integer> getAddedClocks() {
        return null;  //This method was auto-implemented
    }

    @Override
    public void forwardEvent(Object obj) {

    }

    @Override
    public boolean eventHasBeenReceived(MyTextEvent event) {
        return false;
    }

    @Override
    public void addEventToReceived(MyTextEvent event) {

    }

    public void scrambleNetwork() {

    }

}
