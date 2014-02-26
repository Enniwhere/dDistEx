import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent implements Serializable {

    private Map<String, Integer> timestamp;
    private String sender;
    private int offset;

    MyTextEvent(int offset) {
        this.offset = offset;
    }

    public int getOffset() { return offset; }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public Map<String, Integer> getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Map<String, Integer> timestamp)  {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public int getTextLengthChange(){
        return 0;
    }
}
