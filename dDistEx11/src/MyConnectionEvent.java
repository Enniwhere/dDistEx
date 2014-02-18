import java.io.Serializable;

/**
 */
public class MyConnectionEvent implements Serializable {

    private String type;

    public MyConnectionEvent(String type){
         this.type = type;
    }

    public String getType(){
        return type;
    }
}
