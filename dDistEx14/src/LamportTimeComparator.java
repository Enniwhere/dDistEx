import java.util.Comparator;
import java.util.Map;

/**
 */
public class LamportTimeComparator implements Comparator<MyTextEvent> {

    private String index;

    public LamportTimeComparator(String index){
        this.index = index;
    }
    @Override
    public int compare(MyTextEvent e1, MyTextEvent e2) {
        Map<String, Integer> e1Timestamp = e1.getTimestamp();
        Map<String, Integer> e2Timestamp = e2.getTimestamp();
        Integer e1LamportTime = e1Timestamp.get(index);
        Integer e2LamportTime = e2Timestamp.get(index);
        return e1LamportTime.equals(e2LamportTime) ? e1.getSender().compareTo(e2.getSender()) : e2LamportTime.compareTo(e1LamportTime);
    }
}
