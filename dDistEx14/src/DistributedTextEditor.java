import java.util.ArrayList;
import java.util.Map;

/**
 * Created by simon on 2/26/14.
 */
public interface DistributedTextEditor {

    public void connectionClosed(String index);

    public int getPortNumberTextField();

    public String getIPAddress();

    public void replyToDisconnect(String eventReplayerAddress);

    public int getLamportTime(String index);

    public String getLamportIndex();

    public void incrementLamportTime();

    public Map<String, Integer> getTimestamp();

    public void adjustVectorClock(Map<String, Integer> hashMap);

    public ArrayList<MyTextEvent> getEventHistoryInterval(MyTextEvent event);

    public void addEventToHistory(MyTextEvent textEvent);

    public boolean isDebugging();

    public void addToClock(Map<String, Integer> map);

    public void handleSetupConnection(SetupConnectionEvent setupConnectionEvent);

    public void scrambleNetwork(ScrambleEvent scrambleEvent);

    public int getScrambleLamportClock();

    public Map<String, Integer> getAddedClocks();

    public void forwardEvent(Object obj);

    public boolean eventHasBeenReceived(MyTextEvent event);

    public void addEventToReceived(MyTextEvent event);
}
