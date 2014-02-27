import java.util.Comparator;

/**
 * The LamportTimeComparator is utilized by the EventReplayer to compare timestamps on a specific index.
 */
public class LamportTimeComparator implements Comparator<MyTextEvent> {

    private int index;

    public LamportTimeComparator(int index){
        this.index = index;
    }

    @Override
    public int compare(MyTextEvent e1, MyTextEvent e2) {
        double[] e1Timestamp = e1.getTimestamp();
        double[] e2Timestamp = e2.getTimestamp();
        Double e1LamportTime = e1Timestamp[index];
        Double e2LamportTime = e2Timestamp[index];
        return e1LamportTime.compareTo(e2LamportTime);
    }
}
