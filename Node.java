import java.util.*;

public class Node {
    int nodeUID;
    String hostName;
    int listeningPort;
    int LeaderUID;
    int numberOfNodes;
    int fragmentSize;
    HashMap<Integer, NodeAdj> NeighborWeights;
    boolean leader;
    Queue<Messages> messageQueue;
    List<Handler> connectedClients = Collections.synchronizedList(new ArrayList<Handler>());

    public Node(){}

    //Constructor
    public Node(int nodeUID, String hostName, int listeningPort, HashMap<Integer, NodeAdj> NeighborWeights, int numNodes){
        this.nodeUID = nodeUID;
        this.hostName = hostName;
        this.listeningPort = listeningPort;
        this.NeighborWeights = NeighborWeights;
        this.numberOfNodes = numNodes;
        this.LeaderUID = nodeUID;
        this.fragmentSize=1;
        this.messageQueue = new LinkedList<Messages>();
    }

    //Setters
    public void setLeader(){this.leader = true;}
    public void addMessageToQueue(Messages newMessage){messageQueue.add(newMessage);}
    public void setLeader(int newLeader){this.LeaderUID = newLeader;}
    public void setNumberNodes(int numNodes){this.numberOfNodes = numNodes;}
    public void setFragmentSize(int fragmentSize){this.fragmentSize = fragmentSize;}

    //Getters
    public int getNodeUID() {return this.nodeUID;}
    public int getNodeListeningPort() {return this.listeningPort;}
    public int getLeader(){return this.LeaderUID;}
    public int getNumberNodes(){return this.numberOfNodes;}
    public int getFragmentSize(){return this.fragmentSize;}
    public String getNodeHostName() {return this.hostName;}
    public HashMap<Integer, NodeAdj> getNeighbors() {return this.NeighborWeights;}
    public Messages getMessage(){return (Messages) messageQueue.poll();}
    public Messages checkMessage(){return (Messages) messageQueue.peek();}
    public List<Handler> getAllConnectedClients() {return this.connectedClients;}
    

    public void addClient(Handler client) {
        synchronized (connectedClients) {
            connectedClients.add(client);
        }
    } 
}