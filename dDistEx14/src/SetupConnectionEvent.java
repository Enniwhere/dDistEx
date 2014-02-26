import java.util.Map;

public class SetupConnectionEvent extends MyConnectionEvent {
    private String text;
    private Map<String, Integer> map;

    public String getText() {
        return text;
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public SetupConnectionEvent(String text, Map<String, Integer> map) {
        super(ConnectionEventTypes.SETUP_CONNECTION);
        this.text = text;
        this.map = map;
    }
}
