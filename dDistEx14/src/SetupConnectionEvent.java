import java.util.ArrayList;
import java.util.Map;

public class SetupConnectionEvent extends MyConnectionEvent {
    private String text;
    private Map<String, Integer> vectorClock;
    private ArrayList<MyTextEvent> eventHistory;
    private int scrambleLamportClock;

    public String getText() {
        return text;
    }

    public Map<String, Integer> getVectorClock() {
        return vectorClock;
    }

    public int getScrambleLamportClock() { return scrambleLamportClock;}

    public SetupConnectionEvent(String text, Map<String, Integer> vectorClock, ArrayList<MyTextEvent> eventHistory, int scrambleLamportClock) {
        super(ConnectionEventTypes.SETUP_CONNECTION);
        this.text = text;
        this.vectorClock = vectorClock;
        this.scrambleLamportClock = scrambleLamportClock;
        this.eventHistory = new ArrayList<MyTextEvent>(eventHistory);
    }

    public ArrayList<MyTextEvent> getEventHistory() {
        return eventHistory;
    }
}
