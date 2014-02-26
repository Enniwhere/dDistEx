import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by simon on 2/26/14.
 */
public interface DistributedTextEditor {

    public void connectionClosed();

    public int getPortNumber();

    public String getIPAddress();

    public void replyToDisconnect();

    public int getLamportTime(String index);

    public String getLamportIndex();

    public void incrementLamportTime();

    public Map<String, Integer> getTimestamp();

    public void adjustVectorClock(Map<String, Integer> hashMap);

    public ArrayList<MyTextEvent> getEventHistoryInterval(int start, int end, String lamportIndex);

    public void addEventToHistory(MyTextEvent textEvent);

    public boolean isDebugging();

    public void addToClock(Map<String, Integer> map);

    public void replyToInitConnection(InitConnectionEvent initConnectionEvent);

    public void handleSetupConnection(SetupConnectionEvent setupConnectionEvent);
}
