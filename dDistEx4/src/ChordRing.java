import com.sun.org.apache.xpath.internal.SourceTree;

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

    public static void main(String[] args){
        ChordRing ring = new ChordRing(16);
        Node n1 = new Node();
        Node n2 = new Node();
        Node n3 = new Node();
        Node n4 = new Node();
        ring.join(n1);
        ring.join(n2);
        ring.join(n3);
        ring.join(n4);

        System.out.println("Node 1 has the id: " + n1.getId());
        System.out.println("Node 2 has the id: " + n2.getId());
        System.out.println("Node 3 has the id: " + n3.getId());
        System.out.println("Node 4 has the id: " + n4.getId());
        System.out.println("Node 1 looks up the key 4 and got the node with id " + n1.lookup(4).getId());
        System.out.println("Node 1 looks up the key 12 and got the node with id " + n1.lookup(12).getId());
        System.out.println("Node 3 looks up the key 5 and got the node with id " + n3.lookup(5).getId());

        ring.leave(n2.getId());
        System.out.println("Node 4 looks up the key n2.id-1 after n2 has left and now gets the node with id " + n4.lookup(n2.getId()-1).getId());
    }

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
            Random random = new Random();
            while (true){
                int id = random.nextInt(size-1);
                Node keyNode = root.lookup(id);
                if (keyNode.getId() != id){
                    nodes.add(node);
                    node.setId(id);
                    Node keyNodePredecessor = keyNode.getPredecessor();
                    for (int i = keyNodePredecessor.getId()+1; i<= id; i++){
                        keyNode.removeKey(i);
                        node.addKey(i);
                    }
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
        if (!nodes.isEmpty()){
            Node keyNode = nodes.get(0).lookup(id);
            if (keyNode.getId() == id){
                Node successor = keyNode.getSuccessor();
                Node predecessor = keyNode.getPredecessor();
                for (Integer key : keyNode.getKeys()){
                    successor.addKey(key);
                }
                successor.setPredecessor(predecessor);
                predecessor.setSuccessor(successor);
                nodes.remove(keyNode);
                return true;
            }
        }
        return false;
    }

    public int getSize(){
        return size;
    }
}
