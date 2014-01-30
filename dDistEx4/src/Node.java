import java.util.HashSet;
import java.util.Set;

/**
 * Created by simon on 1/29/14.
 */
public class Node {
    private Node successor, predecessor;
    private Set<Integer> keys = new HashSet<Integer>();
    private int id;

    public Node(){
        successor = this;
        predecessor = this;
    }

    public Node lookup(Integer key){
        if (keys.contains(key)){
            return this;
        } else {
            return successor.lookup(key);
        }
    }


    public Node getSuccessor() {
        return successor;
    }

    public void setSuccessor(Node successor) {
        this.successor = successor;
    }

    public Node getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Node predecessor) {
        this.predecessor = predecessor;
    }

    public void setKeys(Set<Integer> keys) {
        this.keys = keys;
    }

    public void addKey(Integer key){
        keys.add(key);
    }

    public void removeKey(Integer key){
        keys.remove(key);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
