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
    private DistributedDocument areaDocument;

    @Before
    public void setUp(){
        areaDocument = new DistributedDocument();
        area1 = new JTextArea(areaDocument, "", 35, 120);
        inputQueue = new LinkedBlockingQueue<Object>();
        objectInput = new ObjectInputStreamStub(inputQueue);
        eventReplayer = new EventReplayer(objectInput,area1,new DistributedTextEditorStub(area1));
        area1.insert("a",0);
        area1.insert("b",1);
        area1.insert("c",2);
        area1.insert("d",3);
    }

    @Test
    public void insertAtHigherIndexThanLocalInsert(){
        area1.insert("1",1);
        MyTextEvent event = new TextInsertEvent(4,"e");
        event.setSender("client");
        inputQueue.add()


    }


}
