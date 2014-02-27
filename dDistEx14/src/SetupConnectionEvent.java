import java.util.Map;

public class SetupConnectionEvent extends MyConnectionEvent {
    private String text;
    private Map<String, Integer> map;
    private int scrambleLamportClock;

    public String getText() {
        return text;
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public int getScrambleLamportClock() { return scrambleLamportClock;}

    public SetupConnectionEvent(String text, Map<String, Integer> map, int scrambleLamportClock) {
        super(ConnectionEventTypes.SETUP_CONNECTION);
        this.text = text;
        this.map = map;
        this.scrambleLamportClock = scrambleLamportClock;
    }
}
