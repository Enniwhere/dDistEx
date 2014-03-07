import java.util.Comparator;

/**
 * Created by simon on 3/7/14.
 */
public class StringDescendingComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        return -o1.compareTo(o2);
    }
}
