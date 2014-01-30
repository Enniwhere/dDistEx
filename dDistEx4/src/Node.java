import java.util.HashSet;
import java.util.Set;

/**
 * Created by simon on 1/29/14.
 */
public class Node {
    private Node succ,pred;
    private Set<Integer> keys = new HashSet<Integer>();
    private int id;

    public Node(int id){
        this.id = id;
        succ = this;
        pred = this;
    }

    public Node lookup(Integer key){
        if (keys.contains(key)){
            return this;
        } else {
            return succ.lookup(key);
        }
    }


    public Node getSucc() {
        return succ;
    }

    public void setSucc(Node succ) {
        this.succ = succ;
    }

    public Node getPred() {
        return pred;
    }

    public void setPred(Node pred) {
        this.pred = pred;
    }

    public void setKeys(Set<Integer> keys) {
        this.keys = keys;
    }

    public int getId() {
        return id;
    }
}
