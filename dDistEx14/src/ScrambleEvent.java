import java.util.ArrayList;
import java.util.Map;

public class ScrambleEvent extends MyConnectionEvent {


    private final Map<String, Integer> addedClocks;

    public String getDeadAddress() {
        return deadAddress;
    }

    private final String deadAddress;
    private int scrambleLamportClock;

    public int getScrambleLamportClock() {
        return scrambleLamportClock;
    }

    public Map<String, Integer> getAddedClocks() {
        return addedClocks;
    }

    public ScrambleEvent(int scrambleLamportClock, Map<String, Integer> addedClocks, String deadAddress) {
        super(ConnectionEventTypes.SCRAMBLE_EVENT);
        this.scrambleLamportClock = scrambleLamportClock;
        this.addedClocks = addedClocks;
        this.deadAddress = deadAddress;
    }

    public ScrambleEvent(int scrambleLamportClock, Map<String, Integer> addedClocks) {
        super(ConnectionEventTypes.SCRAMBLE_EVENT);
        this.scrambleLamportClock = scrambleLamportClock;
        this.addedClocks = addedClocks;
        this.deadAddress = "no_dead_address";
    }
}
