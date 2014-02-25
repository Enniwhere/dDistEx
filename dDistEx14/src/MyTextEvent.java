import java.io.Serializable;

/**
 *
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent implements Serializable {

    private double[] timestamp;
    private int sender;
    private int offset;

    MyTextEvent(int offset) {
        this.offset = offset;
    }

    public int getOffset() { return offset; }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public double[] getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double[] timestamp) {
        this.timestamp = timestamp;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public int getTextLengthChange(){
        return 0;
    }
}
