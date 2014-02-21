public class TextRemoveEvent extends MyTextEvent {

    private int length;

    public TextRemoveEvent(int offset, int length) {
        super(offset);
        this.length = length;
    }


    public int getLength() { return length; }

    public void setLength(int length){
        this.length = length;
    }

    @Override
    public int getTextLengthChange(){
        return -length;
    }
}
