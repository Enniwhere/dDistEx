import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by simon on 1/29/14.
 */
public class ChordRing {

    private ArrayList<Node> nodes = new ArrayList<Node>();
    private Set<Integer> keys = new HashSet<Integer>();
    private int size;

    public ChordRing(int size){
        this.size = size;
        for (int i = 0; i < size; i++) {
            keys.add(new Integer(i));
        }
    }

    public boolean join(Node node){
        if (nodes.isEmpty()){
            nodes.add(node);
            node.setKeys(keys);
            return true;
        } else if (nodes.size() < this.size){
            Node root = nodes.get(0);
            Node keyNode = root.lookup(value % size);

        }
        return false;
    }
}
