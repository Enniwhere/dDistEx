import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static junit.framework.Assert.assertEquals;
public class TestOffsetAdjusting {

    private JTextArea area1;
    private ObjectInput objectInput;
    private LinkedBlockingQueue<Object> inputQueue;
    private EventReplayer eventReplayer;

    @Before
    public void setUp(){
        area1 = new JTextArea(new DistributedDocument(), "", 35, 120);
        inputQueue = new LinkedBlockingQueue<Object>();
        objectInput = new ObjectInputStreamStub(inputQueue);
        eventReplayer = new EventReplayer(objectInput,area1,new DistributedTextEditorStub(area1));
    }

    @Test
    public void insertAtHigherIndexThanLocalInsert(){



    }


}
