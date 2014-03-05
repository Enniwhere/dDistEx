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
        Integer e1Offset = e1.getOffset();
        Integer e2Offset = e2.getOffset();
        /*
        if (e1.getSender().equals(index) && !e2.getSender().equals(index)){
            return -1;
        } else if (e2.getSender().equals(index) && !e1.getSender().equals(index)){
            return 1;
        } */
        if (e1.getSender().equals(e2.getSender()) && e1.getTimestamp().get(e1.getSender()) > e2.getTimestamp().get(e1.getSender())){
            return 1;
        }
        if (e1Offset.equals(e2Offset)){
            if (e1LamportTime.equals(e2LamportTime)) {
                return e1.getSender().compareTo(e2.getSender());
            } else {
                return e1LamportTime.compareTo(e2LamportTime);
            }
        } else {
            return e1Offset.compareTo(e2Offset);
        }

    }
}
