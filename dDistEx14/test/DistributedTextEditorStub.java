import javax.swing.*;
import javax.swing.text.AbstractDocument;

/**
 * Created by simon on 2/26/14.
 */
public class DistributedTextEditorStub extends DistributedTextEditor {

    private final JTextArea area;
    private final DistributedDocument areaDocument;
    private DocumentEventCapturer documentEventCapturer = new DocumentEventCapturer(this);

    public DistributedTextEditorStub(final JTextArea area){
        this.area = area;
        ((AbstractDocument) area.getDocument()).setDocumentFilter(documentEventCapturer);
        areaDocument = (DistributedDocument) area.getDocument();

    }
}
