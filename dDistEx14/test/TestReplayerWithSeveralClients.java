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
        eventReplayer = new EventReplayer(objectInput,area1, callback, "client1");
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

    public void setUpAlt(String client){
        areaDocument = new DistributedDocument();
        area1 = new JTextArea(areaDocument, "", 35, 120);
        Map<String, Integer> clock = new HashMap<String, Integer>();
        clock.put("client1",0);
        clock.put("client2",0);
        clock.put("client3",0);
        clock.put("client4",0);
        DistributedTextEditorStub callback = new DistributedTextEditorStub(clock,client);
        inputQueue = new LinkedBlockingQueue<Object>();
        objectInput = new ObjectInputStreamStub(inputQueue);
        eventReplayer = new EventReplayer(objectInput,area1, callback, client);
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(new DocumentEventCapturer(callback));
        area1.insert("a",0);
        area1.insert("b",1);
        area1.insert("c",2);
        area1.insert("d",3);
        timestamp = new HashMap<String, Integer>();
        timestamp.put("client1",0);
        timestamp.put("client2",0);
        timestamp.put("client3",0);
        timestamp.put("client4",0);
        timestamp.put(client, 4);
    }

    @Test
    public void threeInsertEventsAfterLocalInsertShouldGiveIntuitiveResults(){
        area1.insert("1",1);

        MyTextEvent insert2 = new TextInsertEvent(1,"2");
        insert2.setSender("client2");
        insert2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3 = new TextInsertEvent(1,"3");
        insert3.setSender("client3");
        timestamp.put("client2",0);
        timestamp.put("client3",1);
        insert3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4 = new TextInsertEvent(1,"4");
        insert4.setSender("client4");
        timestamp.put("client3",0);
        timestamp.put("client4", 1);
        insert4.setTimestamp(new HashMap<String, Integer>(timestamp));
        inputQueue.add(insert3);
        inputQueue.add(insert4);
        inputQueue.add(insert2);
        eventReplayer.run();
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } */
        assertEquals("Text after inserting 1234 at position 1 text should be a1234bcd", "a1234bcd", area1.getText());
    }

    @Test
    public void threeInsertEventsAfterLocalInsertShouldGiveIntuitiveResultsAltClient(){
        setUpAlt("client4");

        area1.insert("4",1);

        MyTextEvent insert2 = new TextInsertEvent(1,"2");
        insert2.setSender("client2");
        timestamp.put("client2",1);
        insert2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3 = new TextInsertEvent(1,"3");
        insert3.setSender("client3");
        timestamp.put("client2",0);
        timestamp.put("client3",1);
        insert3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert1 = new TextInsertEvent(1,"1");
        insert1.setSender("client1");
        timestamp.put("client3",0);
        timestamp.put("client1", 1);
        insert1.setTimestamp(new HashMap<String, Integer>(timestamp));
        inputQueue.add(insert3);
        inputQueue.add(insert1);
        inputQueue.add(insert2);
        eventReplayer.run();
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } */
        assertEquals("Text after inserting 1234 at position 1 text should be a1234bcd", "a1234bcd", area1.getText());
    }

    @Test
    public void twoInsertsFromSameClientWithOneFromOtherClientInBetweenBeforeLocalInsert(){
        area1.insert("1",1);
        MyTextEvent insert2 = new TextInsertEvent(1,"2");
        insert2.setSender("client2");
        insert2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3 = new TextInsertEvent(2,"3");
        insert3.setSender("client3");
        timestamp.put("client3",1);
        timestamp.put("client2",2);
        insert3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4 = new TextInsertEvent(2,"4");
        insert4.setSender("client2");
        timestamp.put("client3", 0);
        insert4.setTimestamp(new HashMap<String, Integer>(timestamp));
        inputQueue.add(insert3);
        inputQueue.add(insert2);
        inputQueue.add(insert4);
        eventReplayer.run();
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } */
        assertEquals("Text after inserting 1234 at position 1 text should be a1234bcd", "a1234bcd", area1.getText());
    }

    @Test
    public void clientInsertBeforeInsideAndAfterLocalRemove(){
        area1.replaceRange("",1,3);

        MyTextEvent insert2 = new TextInsertEvent(0,"2");
        insert2.setSender("client2");
        insert2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3 = new TextInsertEvent(2,"3");
        insert3.setSender("client3");
        timestamp.put("client2",0);
        timestamp.put("client3",1);
        insert3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4 = new TextInsertEvent(4,"4");
        timestamp.put("client3", 0);
        insert4.setSender("client4");
        timestamp.put("client4", 1);
        insert4.setTimestamp(new HashMap<String, Integer>(timestamp));
        inputQueue.add(insert4);
        inputQueue.add(insert2);
        inputQueue.add(insert3);
        eventReplayer.run();
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } */
        assertEquals("Text after inserting 2 before, 3 inside and 4 after local remove should be 2ad4", "2ad4", area1.getText());
    }

    @Test
    public void insertBeforeInsideAndAfterRemoveAlternativeClient(){
        setUpAlt("client3");

        area1.insert("3",2);

        MyTextEvent insert2 = new TextInsertEvent(0,"2");
        insert2.setSender("client2");
        timestamp.put("client2",1);
        insert2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4 = new TextInsertEvent(4,"4");
        timestamp.put("client2", 0);
        insert4.setSender("client4");
        timestamp.put("client4", 1);
        insert4.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent remove = new TextRemoveEvent(1,2);
        remove.setSender("client1");
        timestamp.put("client4", 0);
        timestamp.put("client1", 1);
        remove.setTimestamp(new HashMap<String, Integer>(timestamp));
        inputQueue.add(insert4);
        inputQueue.add(insert2);
        inputQueue.add(remove);
        eventReplayer.run();

        assertEquals("Text after inserting 2 before, 3 locally inside and 4 after remove should be 2ad4", "2ad4", area1.getText());
    }

    @Test
    public void severalInsertsFromSeveralClientsOnDifferentOffsetsShouldGiveIntuitiveResults(){
        area1.insert("(1_1)",1);
        area1.insert("(1_3)",3+5);

        MyTextEvent insert2_0 = new TextInsertEvent(0,"(2_0)");
        insert2_0.setSender("client2");
        timestamp.put("client2",1);
        insert2_0.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert2_3 = new TextInsertEvent(3+5,"(2_3)");
        insert2_3.setSender("client2");
        timestamp.put("client2",2);
        insert2_3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3_1 = new TextInsertEvent(1,"(3_1)");
        insert3_1.setSender("client3");
        timestamp.put("client2",0);
        timestamp.put("client3",1);
        insert3_1.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert3_2 = new TextInsertEvent(2+5,"(3_2)");
        insert3_2.setSender("client3");
        timestamp.put("client3",2);
        insert3_2.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4_4 = new TextInsertEvent(4,"(4_4)");
        timestamp.put("client3", 0);
        insert4_4.setSender("client4");
        timestamp.put("client4", 1);
        insert4_4.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert4_1 = new TextInsertEvent(1,"(4_1)");
        insert4_1.setSender("client4");
        timestamp.put("client4", 2);
        insert4_1.setTimestamp(new HashMap<String, Integer>(timestamp));

        inputQueue.add(insert4_1);
        inputQueue.add(insert4_4);
        inputQueue.add(insert3_2);
        inputQueue.add(insert3_1);
        inputQueue.add(insert2_0);
        inputQueue.add(insert2_3);
        eventReplayer.run();

        assertEquals("Text after inserting the events should be (2_0)a(1_1)(3_1)(4_1)b(3_2)c(1_3)(2_3)d(4_4)", "(2_0)a(1_1)(3_1)(4_1)b(3_2)c(1_3)(2_3)d(4_4)", area1.getText());

    }

    @Test
    public void severalInsertsFromSeveralClientsOnDifferentOffsetsShouldGiveIntuitiveResultsAltClient(){
        setUpAlt("client3");

        area1.insert("(3_1)",1);
        area1.insert("(3_2)",2+5);

        MyTextEvent insert2_0 = new TextInsertEvent(0,"(2_0)");
        insert2_0.setSender("client2");
        timestamp.put("client2",1);
        insert2_0.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert2_3 = new TextInsertEvent(3+5,"(2_3)");
        insert2_3.setSender("client2");
        timestamp.put("client2",2);
        insert2_3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert1_1 = new TextInsertEvent(1,"(1_1)");
        insert1_1.setSender("client1");
        timestamp.put("client2",0);
        timestamp.put("client1",1);
        insert1_1.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert1_3 = new TextInsertEvent(3+5,"(1_3)");
        insert1_3.setSender("client1");
        timestamp.put("client1",2);
        insert1_3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4_4 = new TextInsertEvent(4,"(4_4)");
        timestamp.put("client1", 0);
        insert4_4.setSender("client4");
        timestamp.put("client4", 1);
        insert4_4.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert4_1 = new TextInsertEvent(1,"(4_1)");
        insert4_1.setSender("client4");
        timestamp.put("client4", 2);
        insert4_1.setTimestamp(new HashMap<String, Integer>(timestamp));

        inputQueue.add(insert4_1);
        inputQueue.add(insert4_4);
        inputQueue.add(insert1_1);
        inputQueue.add(insert1_3);
        inputQueue.add(insert2_0);
        inputQueue.add(insert2_3);
        eventReplayer.run();

        assertEquals("Text after inserting the events should be (2_0)a(1_1)(3_1)(4_1)b(3_2)c(1_3)(2_3)d(4_4)", "(2_0)a(1_1)(3_1)(4_1)b(3_2)c(1_3)(2_3)d(4_4)", area1.getText());

    }

    @Test
    public void severalInsertsAndARemoveFromSeveralClientsOnDifferentOffsetsShouldGiveIntuitiveResults(){
        area1.insert("(1_1)",1);
        area1.insert("(1_3)",3+5);

        MyTextEvent insert2_0 = new TextInsertEvent(0,"(2_0)");
        insert2_0.setSender("client2");
        timestamp.put("client2",1);
        insert2_0.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert2_3 = new TextInsertEvent(3+5,"(2_3)");
        insert2_3.setSender("client2");
        timestamp.put("client2",2);
        insert2_3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert3_1 = new TextInsertEvent(1,"(3_1)");
        insert3_1.setSender("client3");
        timestamp.put("client2",0);
        timestamp.put("client3",1);
        insert3_1.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent remove3_1_7 = new TextRemoveEvent(1,2+5);
        remove3_1_7.setSender("client3");
        timestamp.put("client3",2);
        remove3_1_7.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4_4 = new TextInsertEvent(4,"(4_4)");
        timestamp.put("client3", 0);
        insert4_4.setSender("client4");
        timestamp.put("client4", 1);
        insert4_4.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert4_1 = new TextInsertEvent(1,"(4_1)");
        insert4_1.setSender("client4");
        timestamp.put("client4", 2);
        insert4_1.setTimestamp(new HashMap<String, Integer>(timestamp));

        inputQueue.add(insert4_1);
        inputQueue.add(insert4_4);
        inputQueue.add(remove3_1_7);
        inputQueue.add(insert3_1);
        inputQueue.add(insert2_0);
        inputQueue.add(insert2_3);
        eventReplayer.run();

        assertEquals("Text after inserting the events should be (2_0)a(1_1)(1_3)(2_3)d(4_4)", "(2_0)a(1_1)(1_3)(2_3)d(4_4)", area1.getText());

    }

    @Test
    public void severalInsertsAndARemoveFromSeveralClientsOnDifferentOffsetsShouldGiveIntuitiveResultsAltClient(){
        setUpAlt("client3");

        area1.insert("(3_1)",1);
        area1.replaceRange("",1,8);

        MyTextEvent insert2_0 = new TextInsertEvent(0,"(2_0)");
        insert2_0.setSender("client2");
        timestamp.put("client2",1);
        insert2_0.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert2_3 = new TextInsertEvent(3+5,"(2_3)");
        insert2_3.setSender("client2");
        timestamp.put("client2",2);
        insert2_3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert1_1 = new TextInsertEvent(1,"(1_1)");
        insert1_1.setSender("client1");
        timestamp.put("client2",0);
        timestamp.put("client1",1);
        insert1_1.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert1_3 = new TextInsertEvent(3+5,"(1_3)");
        insert1_3.setSender("client1");
        timestamp.put("client1",2);
        insert1_3.setTimestamp(new HashMap<String, Integer>(timestamp));

        MyTextEvent insert4_4 = new TextInsertEvent(4,"(4_4)");
        timestamp.put("client1", 0);
        insert4_4.setSender("client4");
        timestamp.put("client4", 1);
        insert4_4.setTimestamp(new HashMap<String, Integer>(timestamp));
        MyTextEvent insert4_1 = new TextInsertEvent(1,"(4_1)");
        insert4_1.setSender("client4");
        timestamp.put("client4", 2);
        insert4_1.setTimestamp(new HashMap<String, Integer>(timestamp));

        inputQueue.add(insert4_1);
        inputQueue.add(insert1_3);
        inputQueue.add(insert2_0);
        inputQueue.add(insert2_3);
        inputQueue.add(insert4_4);
        inputQueue.add(insert1_1);

        eventReplayer.run();

        assertEquals("Text after inserting the events should be (2_0)a(1_1)(1_3)(2_3)d(4_4)", "(2_0)a(1_1)(1_3)(2_3)d(4_4)", area1.getText());

    }


}
