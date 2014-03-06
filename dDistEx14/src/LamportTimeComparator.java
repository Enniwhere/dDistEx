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
        if (e2.getSender().equals(e1.getSender()) && e2.getTimestamp().get(e1.getSender()) > e1.getTimestamp().get(e2.getSender())){
            return -1;
        }
        if (e1 instanceof TextRemoveEvent && e2.isIgnored() && e1.getOffset() < e2.getOffset() && (e1Offset != e2Offset || e1.getSender().compareTo(e2.getSender()) < 0) && e2.getOffset() < e1.getOffset() + ((TextRemoveEvent) e1).getLength()){
            return -1;
        } else if (e2 instanceof TextRemoveEvent && e1.isIgnored() && e2.getOffset() < e1.getOffset() && (e2Offset != e1Offset || e2.getSender().compareTo(e1.getSender()) < 0) && e1.getOffset() < e2.getOffset() + ((TextRemoveEvent) e2).getLength()){
            return 1;
        }

        if (e1Offset.equals(e2Offset)){
            if (e1 instanceof TextRemoveEvent && !(e2 instanceof TextRemoveEvent)) {
                return -1;
            } else if (e2 instanceof TextRemoveEvent && !(e1 instanceof TextRemoveEvent)){
                return 1;
            }
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
