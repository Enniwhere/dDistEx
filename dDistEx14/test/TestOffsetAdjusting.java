import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.io.ObjectInput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static junit.framework.Assert.assertEquals;
public class TestOffsetAdjusting {

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
        clock.put("server",0);
        clock.put("client",0);
        DistributedTextEditorStub callback = new DistributedTextEditorStub(clock,"server");
        inputQueue = new LinkedBlockingQueue<Object>();
        objectInput = new ObjectInputStreamStub(inputQueue);
        eventReplayer = new EventReplayer(objectInput,area1, callback, "server");
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(new DocumentEventCapturer(callback));
        area1.insert("a",0);
        area1.insert("b",1);
        area1.insert("c",2);
        area1.insert("d",3);
        timestamp = new HashMap<String, Integer>();
        timestamp.put("server",4);
        timestamp.put("client",1);
    }

    @Test
    public void insertAtHigherIndexThanLocalInsert(){
        area1.insert("1",1);
        MyTextEvent event = new TextInsertEvent(4,"e");
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after inserting 1 at position 1 and e at position 4 should be a1bcde", "a1bcde", area1.getText());

    }

    @Test
    public void insertAtHigherIndexThanLocalRemove(){
        area1.replaceRange("",1,2);
        MyTextEvent event = new TextInsertEvent(4,"e");
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 1 and inserting e at position 4 should be acde", "acde", area1.getText());
    }

    @Test
    public void removeAtHigherIndexThanLocalInsert(){
        area1.insert("1",1);
        MyTextEvent event = new TextRemoveEvent(3,1);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after inserting 1 at position 1 and removing at position 3 should be a1bc", "a1bc", area1.getText());
    }

    @Test
    public void removeAtHigherIndexThanLocalRemove(){
        area1.replaceRange("",1,2);
        MyTextEvent event = new TextRemoveEvent(3,1);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 1 and removing at position 3 should be ac", "ac", area1.getText());
    }

    @Test
    public void insertAtSameOffsetAsLocalInsert(){
        area1.insert("1",1);
        MyTextEvent event = new TextInsertEvent(1,"2");
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after inserting 1 at position 1 and e at position 4 should be a21bcd", "a21bcd", area1.getText());
    }

    @Test
    public void removeAtSameOffsetAsLocalInsert(){
        area1.insert("1",2);
        MyTextEvent event = new TextRemoveEvent(1,1);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after inserting 1 at position 2 and removing at position 1 should be a1cd", "a1cd", area1.getText());
    }

    @Test
    public void insertAtSameOffsetAsLocalRemove(){
        area1.replaceRange("", 1, 2);
        MyTextEvent event = new TextInsertEvent(2,"1");
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after remove from position 1 and inserting 1 at position 2 should be a1cd", "a1cd", area1.getText());
    }

    @Test
    public void removeAtSameOffsetAsLocalRemove(){
        area1.replaceRange("", 1, 2);
        MyTextEvent event = new TextRemoveEvent(2,1);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 2 and removing at position 1 should be ad", "ad", area1.getText());
    }

    @Test
    public void removeInsideLocalRemoveShouldBeIgnored(){
        area1.replaceRange("", 1, 3);
        MyTextEvent event = new TextRemoveEvent(2,1);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 1-3 and removing at position 2 should be ad", "ad", area1.getText());
    }

    @Test
    public void insertInsideLocalRemoveShouldBeIgnored(){
        area1.replaceRange("", 1, 3);
        MyTextEvent event = new TextInsertEvent(2,"2");
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 1-3 and inserting 2 at position 2 should be ad", "ad", area1.getText());
    }

    @Test
    public void removeContainingLocalRemoveShouldGiveIntuitiveResults(){
        area1.replaceRange("",2,3);
        MyTextEvent event = new TextRemoveEvent(1,2);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 2 and removing at position 1-3 should be ad", "ad", area1.getText());
    }

    @Test
    public void removeContainingLocalInsertShouldGiveIntuitiveResults(){
        area1.insert("2",2);
        MyTextEvent event = new TextRemoveEvent(1,2);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after inserting 2 at position 2 and removing at position 1-3 should be ad", "ad", area1.getText());
    }

    @Test
    public void removeOverlappingLocalRemoveInFrontShouldGiveIntuitiveResults(){
        area1.replaceRange("", 2, 4);
        MyTextEvent event = new TextRemoveEvent(1,2);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 2-4 and removing at position 1-3 should be a", "a", area1.getText());
    }

    @Test
    public void removeOverlappingLocalRemoveInEndShouldGiveIntuitiveResults(){
        area1.replaceRange("", 1, 3);
        MyTextEvent event = new TextRemoveEvent(2,2);
        event.setSender("client");
        event.setTimestamp(timestamp);
        inputQueue.add(event);
        eventReplayer.run();
        assertEquals("Text after removing at position 1-3 and removing at position 2-4 should be a", "a", area1.getText());
    }



}

