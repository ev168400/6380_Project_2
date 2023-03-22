import java.io.Serializable;
import java.util.*;

//Template to create messages that will be sent from node to node
public class Messages implements Serializable {
    int highestUID;
    int phase;
    int UIDofSender;
    Type typeOfMessage;

    public Messages(){}

    public Messages(int highestUID, int phase, int UIDofSender, Type typeOfMessage){
        this.highestUID = highestUID;
        this.phase = phase;
        this.UIDofSender = UIDofSender ;
        this.typeOfMessage = typeOfMessage;
    }

    //getters
    public int getHighestUID(){return highestUID;}
    public int getPhase(){return phase;}
    public int getUIDofSender(){return UIDofSender;}
    public Type getTypeOfMessage(){return typeOfMessage;}
}
