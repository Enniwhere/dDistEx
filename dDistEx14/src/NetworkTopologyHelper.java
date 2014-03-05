import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NetworkTopologyHelper {

    public NetworkTopologyHelper(){

    }

    public ArrayList<String> selectThreePeers(String Address, Map<String, Integer> vectorClockMap) {
        Map<String, Integer> clockMap = new HashMap<String, Integer>(vectorClockMap);
        ArrayList<String> res = new ArrayList<String>();
        clockMap.remove(Address);
        if(clockMap.size() <= 3) {
            for(String s : clockMap.keySet()) {
                res.add(s);
            }
        } else {
            Random rand = new Random();
            ArrayList<String> listOfIP = new ArrayList<String>();
            for (String s : clockMap.keySet()) listOfIP.add(s);
            for (int i = 0; i < 3; i++) {
                String s = listOfIP.get(rand.nextInt(listOfIP.size()));
                res.add(s);
                listOfIP.remove(s);
            }
        }
        return res;
    }

}
