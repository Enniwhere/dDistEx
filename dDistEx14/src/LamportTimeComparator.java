import java.util.*;

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

        ArrayList<String> sortedKeys = new ArrayList<String>(e1Timestamp.keySet());
        Collections.sort(sortedKeys,new StringDescendingComparator());
        // We get the events from the given index first, because we have to
        if (e1.getSender().equals(index) && !e2.getSender().equals(index)){
            return -1;
        } else if (e2.getSender().equals(index) && !e1.getSender().equals(index)){
            return 1;
        }
        for (String key : sortedKeys){
            if (e1Timestamp.get(key).compareTo(e2Timestamp.get(key)) != 0){
                return e1Timestamp.get(key).compareTo(e2Timestamp.get(key));
            }
        }
        return 0;
        /*
        Set<String> keysetTemp = e1Timestamp.keySet();
        for (String s : keysetTemp){
            if (s.compareTo(index) < 0){
                index = s;
            }
        }
        Integer e1LamportTime = e1Timestamp.get(index);
        Integer e2LamportTime = e2Timestamp.get(index);   */

        /*
        if (e1.getSender().equals(index) && !e2.getSender().equals(index)){
            return -1;
        } else if (e2.getSender().equals(index) && !e1.getSender().equals(index)){
            return 1;
        }
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
        } */

    }
}
