import java.util.Map;

public class InitConnectionEvent extends MyConnectionEvent {

    private final Map<String, Integer> map;

    public Map<String, Integer> getMap() {
        return map;
    }

    public InitConnectionEvent(Map<String, Integer> map) {
        super(ConnectionEventTypes.INIT_CONNECTION);
        this.map = map;
    }

}
