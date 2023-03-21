import java.util.*;

public class Node {
    int nodeUID;
    String hostName;
    int listeningPort;
    HashMap<Integer, ArrayList<Node>> Neighbors;
    HashMap<Integer, NodeAdj> NeighborWeights;
    boolean leader;
    //Queue<Messages> messageQueue;
    //List<Handler> connectedClients = Collections.synchronizedList(new ArrayList<Handler>());

    public Node(){}

    //Constructor
    public Node(int nodeUID, String hostName, int listeningPort, HashMap<Integer, ArrayList<Node>> Neighbors, HashMap<Integer, NodeAdj> NeighborWeights){
        this.nodeUID = nodeUID;
        this.hostName = hostName;
        this.listeningPort = listeningPort;
        this.Neighbors = Neighbors;
        this.NeighborWeights = NeighborWeights;
        //this.messageQueue = new LinkedList<Messages>();
    }

    //Setters
    public void setLeader(){this.leader = true;}
    //public void addMessageToQueue(Messages newMessage){messageQueue.add(newMessage);}

    //Getters
    public int getNodeUID() {return this.nodeUID;}
    public int getNodeListeningPort() {return this.listeningPort;}
    public String getNodeHostName() {return this.hostName;}
    public HashMap<Integer, ArrayList<Node>> getNeighbors() {return this.Neighbors;}
    //public Messages getMessage(){return (Messages) messageQueue.poll();}
    //public Messages checkMessage(){return (Messages) messageQueue.peek();}
    //public List<Handler> getAllConnectedClients() {return this.connectedClients;}
    
/* 
    public void addClient(Handler client) {
        synchronized (connectedClients) {
            connectedClients.add(client);
        }
    } */
}