import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * Created by simon on 2/19/14.
 */
public class DistributedDocument extends PlainDocument {

    private boolean filterEnabled = true;

    public void disableFilter(){
        filterEnabled = false;
    }

    public void enableFilter(){
        filterEnabled = true;
    }

    @Override
    public DocumentFilter getDocumentFilter(){
        if (filterEnabled){
            return super.getDocumentFilter();
        } else {
            return new DocumentFilter();
        }
    }
}
