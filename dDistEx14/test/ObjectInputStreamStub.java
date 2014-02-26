import java.io.IOException;
import java.io.ObjectInput;
import java.util.concurrent.BlockingQueue;

/**
 * Created by simon on 2/25/14.
 */
public class ObjectInputStreamStub implements ObjectInput {

    private BlockingQueue<Object> queue;

    public ObjectInputStreamStub(BlockingQueue<Object> queue){
        this.queue = queue;
    }
    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        try {
            return queue.take();  //This method was auto-implemented
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int read() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public int read(byte[] b) throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public long skip(long n) throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public int available() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public void close() throws IOException {
        //This method was auto-implemented
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        //This method was auto-implemented
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        //This method was auto-implemented
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public boolean readBoolean() throws IOException {
        return false;  //This method was auto-implemented
    }

    @Override
    public byte readByte() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public short readShort() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public char readChar() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public int readInt() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public long readLong() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public float readFloat() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public double readDouble() throws IOException {
        return 0;  //This method was auto-implemented
    }

    @Override
    public String readLine() throws IOException {
        return null;  //This method was auto-implemented
    }

    @Override
    public String readUTF() throws IOException {
        return null;  //This method was auto-implemented
    }
}
