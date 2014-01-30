import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
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

            return true;
        } else if (nodes.size() < this.size){
            Node root = nodes.get(0);
            Random random = new Random();
            while (true){
                int id = random.nextInt(size-1);
                Node keyNode = root.lookup(id);
                if (keyNode.getId() != id){
                    nodes.add(node);
                    node.setId(id);
                    Node keyNodePredecessor = keyNode.getPredecessor();
                    node.setPredecessor(keyNodePredecessor);
                    node.setSuccessor(keyNode);
                    keyNode.setPredecessor(node);
                    keyNodePredecessor.setSuccessor(node);
                    return true;
                }
            }


        }
        return false;
    }

    public boolean leave(int id){
        if (nodes.contains(node)){

        }
        return false;
    }
}
