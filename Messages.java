import java.io.Serializable;
import java.util.*;

//Template to create messages that will be sent from node to node
public class Messages implements Serializable {
    int LeaderUID;
    int phase;
    int UIDofSender;
    int weight; 
    int intendedRecipient;
    Type typeOfMessage;

    public Messages(){}

    public Messages(int LeaderUID, int phase, int UIDofSender, int weight, int intendedRecipient, Type typeOfMessage){
        this.LeaderUID = LeaderUID;
        this.phase = phase;
        this.UIDofSender = UIDofSender;
        this.weight = weight;
        this.intendedRecipient = intendedRecipient;
        this.typeOfMessage = typeOfMessage;
    }

    //getters
    public int getLeaderUID(){return LeaderUID;}
    public int getPhase(){return phase;}
    public int getUIDofSender(){return UIDofSender;}
    public int getWeight(){return weight;}
    public int getIntendedRecipient(){return intendedRecipient;}
    public Type getTypeOfMessage(){return typeOfMessage;}
}
