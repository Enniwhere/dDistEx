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
        // Normally, strings are sorted in ascending alphabetical order. In our case we want to reverse this sorting.
        Collections.sort(sortedKeys,new StringDescendingComparator());
        // We get the events from the given index first, because we have to see if this event has been moved by earlier
        // events from same client which the other clients haven't seen.
        if (e1.getSender().equals(index) && !e2.getSender().equals(index)){
            return -1;
        } else if (e2.getSender().equals(index) && !e1.getSender().equals(index)){
            return 1;
        }

        // We sort the events by their timestamps, starting with the index for the largest client id. In this way,
        // we make sure that the events come in the same order they were created in, in their local time with the
        // lowest client id happening first.
        for (String key : sortedKeys){
            if (e1Timestamp.get(key).compareTo(e2Timestamp.get(key)) != 0){
                return e1Timestamp.get(key).compareTo(e2Timestamp.get(key));
            }
        }
        return 0;

    }
}
