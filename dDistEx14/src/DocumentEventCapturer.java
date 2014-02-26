import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * This class captures and remembers the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions
 * on what can be written in a buffer. In out case we just use it to see all
 * the events and make a copy. 
 *
 * @author Jesper Buus Nielsen
 *
 */
public class DocumentEventCapturer extends DocumentFilter {

    private final DistributedTextEditor callback;
    /*
         * We are using a blocking queue for two reasons:
         * 1) They are thread safe, i.e., we can have two threads add and take elements
         *    at the same time without any race conditions, so we do not have to do
         *    explicit synchronization.
         * 2) It gives us a member take() which is blocking, i.e., if the queue is
         *    empty, then take() will wait until new elements arrive, which is what
         *    we want, as we then don't need to keep asking until there are new elements.
         */
    protected LinkedBlockingQueue<MyTextEvent> eventHistory = new LinkedBlockingQueue<MyTextEvent>();

    public DocumentEventCapturer(DistributedTextEditor distributedTextEditor) {
         this.callback = distributedTextEditor;
    }

    /**
     * If the queue is empty, then the call will block until an element arrives.
     * If the thread gets interrupted while waiting, we throw InterruptedException.
     *
     * @return Head of the recorded event queue. 
     */
    public MyTextEvent take() throws InterruptedException {
        return eventHistory.take();
    }

    public void insertString(FilterBypass filterBypass, int offset,
                             String string, AttributeSet attributeSet)
            throws BadLocationException {
	
	    // We lock the document from other changes while we generate the event, increment the clock,
        // set the timestamp and sender of the event, adds it to the correct histories and insert
        // the string into the textarea. This is to avoid concurrency errors between creating the event
        // and modifying the area accordingly.
        synchronized (filterBypass.getDocument()){
            TextInsertEvent insertEvent = new TextInsertEvent(offset, string);
            callback.incrementLamportTime();
            System.out.println(callback.getLamportTime(callback.getLamportIndex()));
            insertEvent.setTimestamp(callback.getTimestamp());
            insertEvent.setSender(callback.getLamportIndex());
            callback.addEventToHistory(insertEvent);
            eventHistory.add(insertEvent);
            super.insertString(filterBypass, offset, string, attributeSet);
        }
    }

    public void remove(FilterBypass filterBypass, int offset, int length)
            throws BadLocationException {

        // We lock the document from other changes while we generate the event, increment the clock,
        // set the timestamp and sender of the event, add it to the correct histories and remove
        // the text from the textarea. This is to avoid concurrency errors between creating the event
        // and modifying the area accordingly.
        synchronized (filterBypass.getDocument()){
            TextRemoveEvent removeEvent = new TextRemoveEvent(offset, length);
            callback.incrementLamportTime();
            removeEvent.setTimestamp(callback.getTimestamp());
            removeEvent.setSender(callback.getLamportIndex());
            callback.addEventToHistory(removeEvent);
            eventHistory.add(removeEvent);
            super.remove(filterBypass, offset, length);
        }
    }

    public void replace(FilterBypass filterBypass, int offset,
                        int length,
                        String str, AttributeSet attributeSet)
            throws BadLocationException {

        // We lock the document from other changes while we generate the event, increment the clock,
        // set the timestamp and sender of the event, add it to the correct histories and remove/insert
        // the text from/into the textarea. This is to avoid concurrency errors between creating the events
        // and modifying the area accordingly.
        synchronized (filterBypass.getDocument()){
            if (length > 0) {
                TextRemoveEvent removeEvent = new TextRemoveEvent(offset, length);
                callback.incrementLamportTime();
                removeEvent.setTimestamp(callback.getTimestamp());
                removeEvent.setSender(callback.getLamportIndex());
                callback.addEventToHistory(removeEvent);
                eventHistory.add(removeEvent);
            }
            TextInsertEvent insertEvent = new TextInsertEvent(offset, str);
            callback.incrementLamportTime();
            insertEvent.setTimestamp(callback.getTimestamp());
            insertEvent.setSender(callback.getLamportIndex());
            callback.addEventToHistory(insertEvent);
            eventHistory.add(insertEvent);
            super.replace(filterBypass, offset, length, str, attributeSet);
        }
    }

    public void clearEventHistory() {
       eventHistory = new LinkedBlockingQueue<MyTextEvent>();
    }
}
