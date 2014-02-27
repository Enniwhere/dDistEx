
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    @Override
    public void connectionClosed() {
        System.out.println("Tried to close the replayer thread");
    }

    @Override
    public int getPortNumber() {
        return 0;  //This method was auto-implemented
    }

    @Override
    public String getIPAddress() {
        return null;  //This method was auto-implemented
    }

    @Override
    public void replyToDisconnect() {
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
    public synchronized void incrementLamportTime() {
        vectorClockMap.put(lamportIndex, getLamportTime(lamportIndex) + 1);
    }

    @Override
    public Map<String, Integer> getTimestamp() {
        return new HashMap<String, Integer>(vectorClockMap);
    }

    @Override
    public synchronized void adjustVectorClock(Map<String, Integer> hashMap) {
        for (String s : hashMap.keySet()) {
            vectorClockMap.put(s, Math.max(vectorClockMap.get(s), hashMap.get(s)));
        }
    }

    @Override
    public ArrayList<MyTextEvent> getEventHistoryInterval(int start, int end, String lamportIndex) {
        ArrayList<MyTextEvent> res = new ArrayList<MyTextEvent>();
        synchronized (eventHistory) {
            for (MyTextEvent event : eventHistory) {
                int time = event.getTimestamp().get(lamportIndex);
                if (time > start && time <= end) {
                    res.add(event);
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
    public boolean addToClock(Map<String, Integer> map) {
        //TODO: IMPLEMENT THIS
        return false;
    }

    @Override
    public void replyToInitConnection(InitConnectionEvent initConnectionEvent) {
        //TODO: IMPLEMENTS THIS
    }

    @Override
    public void handleSetupConnection(SetupConnectionEvent setupConnectionEvent) {
        //TODO: IMPLEMENTS THIS
    }

}
