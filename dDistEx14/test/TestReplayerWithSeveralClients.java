import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.io.ObjectInput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static junit.framework.Assert.assertEquals;

/**
 * Created by simon on 2/27/14.
 */
public class TestReplayerWithSeveralClients {

    private JTextArea area1;
    private ObjectInput objectInput;
    private LinkedBlockingQueue<Object> inputQueue;
    private EventReplayer eventReplayer;
    private DistributedDocument areaDocument;
    private Map<String, Integer> timestamp;

    @Before
    public void setUp(){
        areaDocument = new DistributedDocument();
        area1 = new JTextArea(areaDocument, "", 35, 120);
        Map<String, Integer> clock = new HashMap<String, Integer>();
        clock.put("client1",0);
        clock.put("client2",0);
        clock.put("client3",0);
        clock.put("client4",0);
        DistributedTextEditorStub callback = new DistributedTextEditorStub(clock,"client1");
        inputQueue = new LinkedBlockingQueue<Object>();
        objectInput = new ObjectInputStreamStub(inputQueue);
        eventReplayer = new EventReplayer(objectInput,area1, callback);
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(new DocumentEventCapturer(callback));
        area1.insert("a",0);
        area1.insert("b",1);
        area1.insert("c",2);
        area1.insert("d",3);
        timestamp = new HashMap<String, Integer>();
        timestamp.put("client1",4);
        timestamp.put("client2",1);
        timestamp.put("client3",0);
        timestamp.put("client4",0);
    }

    @Test
    public void threeInsertEventsAfterLocalInsertShouldGiveIntuitiveResults(){
        area1.insert("1",1);

        MyTextEvent insert2 = new TextInsertEvent(1,"2");
        insert2.setSender("client2");
        insert2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3 = new TextInsertEvent(1,"3");
        insert3.setSender("client3");
        timestamp.put("client3",1);
        insert3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4 = new TextInsertEvent(1,"4");
        insert4.setSender("client4");
        timestamp.put("client4", 1);
        insert4.setTimestamp(new HashMap<String, Integer>(timestamp));
        inputQueue.add(insert2);
        inputQueue.add(insert3);
        inputQueue.add(insert4);
        eventReplayer.run();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("Text after inserting 1432 at position 1 text should be a1432bcd", "a1432bcd", area1.getText());
    }
}
