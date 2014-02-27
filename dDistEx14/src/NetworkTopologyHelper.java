import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NetworkTopologyHelper {
    private String myAddress = "uno";
    private ArrayList<String> connections = new ArrayList<String>();

    public NetworkTopologyHelper(){

    }

    private void selectThreePeers() {
        Map<String, Integer> vectorClockMap = getVectorClockMap();
        vectorClockMap.remove(myAddress);
        ArrayList<String> addresses = new ArrayList<String>(vectorClockMap.size());
        Random rand = new Random();
        if(addresses.size() < 3){
            for(String s : addresses){
                connectTo(s);
            }
        }
        else{
            for (int i = 0; i < 3; ) {
                if(connectTo(addresses.get(rand.nextInt(vectorClockMap.size())))){
                    i++;
                }
            }
        }
    }

    private boolean connectTo(String s){
        if (connections.contains(s)){
            return false;
        }
        return true;
    }

    private Map<String, Integer> getVectorClockMap(){
        Map <String, Integer> vectorClockMap = new HashMap<String, Integer>();
        vectorClockMap.put("uno", 1);
        vectorClockMap.put("dos", 2);
        vectorClockMap.put("tres", 3);
        vectorClockMap.put("cuatro", 4);
        vectorClockMap.put("cinco", 5);
        vectorClockMap.put("seis", 6);
        vectorClockMap.put("siete", 7);
        vectorClockMap.put("ocho",8);
        return vectorClockMap;
    }

}
