import java.util.Comparator;

/**
 * Created by simon on 3/9/14.
 */
public class InvertedLamportTimeComparator implements Comparator<MyTextEvent> {

    private LamportTimeComparator delegate;

    public InvertedLamportTimeComparator(String index){
        this.delegate = new LamportTimeComparator(index);

    }

    @Override
    public int compare(MyTextEvent o1, MyTextEvent o2) {
        return -delegate.compare(o1,o2);
    }
}
