import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class parser {
    final static HashMap<Integer, Node> nodeList = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Node node = new Node();
        HashMap<Integer, ArrayList<Node>> NeighborNodes = new HashMap<>();
        

        // TODO remove hard code after parser is working
        String path = "config.txt";
        BufferedReader b = new BufferedReader(new FileReader(path));

        String readLine = "";
        readLine = b.readLine();

        // Ignores any empty lines or commented lines
        while (readLine.isEmpty() || readLine.charAt(0) == '#') {
            readLine = b.readLine();
        }

        // Readline holds the number of nodes in the system
        int numberOfNodes = Integer.parseInt(readLine);
        readLine = b.readLine();

        // Ignores any empty lines or commented lines
        while (readLine.isEmpty() || readLine.charAt(0) == '#') {
            readLine = b.readLine();
        }

        // Reads in the information from the first item
        readLine.trim();
        int UID = 0;
        String Hostname = "";
        int Port = 0;

        // Split the line based on spaces
        String[] s = readLine.split("\\s+");

        // Assign the variables from the line
        for (int j = 0; j < s.length; j++) {
            UID = Integer.parseInt(s[0]);
            Hostname = s[1];
            Port = Integer.parseInt(s[2]);
        }
        HashMap<Integer, NodeAdj> NeighborWeight = new HashMap<>();
        Node addNode = new Node(UID, Hostname, Port, NeighborNodes, NeighborWeight);
        nodeList.put(UID, addNode);

        // Initiate the neighbor list for each node by entering all nodes in the
        // nodeList
        nodeList.get(UID).Neighbors.put(UID, new ArrayList<>());

        // Loop through the rest
        for (int i = 1; i < numberOfNodes; i++) {
            // The system uses Trim to handle leading and trailing white space
            readLine = b.readLine().trim();

            // Split the line based on spaces
            s = readLine.split("\\s+");

            // Assign the variables from the line
            for (int j = 0; j < s.length; j++) {
                UID = Integer.parseInt(s[0]);
                Hostname = s[1];
                Port = Integer.parseInt(s[2]);
            }
            HashMap<Integer, NodeAdj> NeighborWeight2 = new HashMap<>();
            addNode = new Node(UID, Hostname, Port, NeighborNodes, NeighborWeight2);
            nodeList.put(UID, addNode);

            // Initiate the neighbor list for each node by entering all nodes in the
            // nodeList
            nodeList.get(UID).Neighbors.put(UID, new ArrayList<>());
        }

        readLine = b.readLine();

        // Ignores any empty lines or commented lines
        while (readLine.isEmpty() || readLine.charAt(0) == '#') {
            readLine = b.readLine();
        }

        // Set up the neighbors and their weights
        readLine.trim();
        // S[0] = (#,#)
        // S[1] = weight
        s = readLine.split("\\s+");

        int weight = Integer.parseInt(s[1]);
        // S[0] smaller UID with (
        // s{1] larger UID with )
        s = s[0].split(",");
        int smallerUID = Integer.parseInt(s[0].substring(1));
        int largerUID = Integer.parseInt(s[1].substring(0, s[1].length() - 1));

        // Create an adj node
        NodeAdj nodeAdjSmall = new NodeAdj(smallerUID, weight, nodeList.get(smallerUID));
        NodeAdj nodeAdjLarge = new NodeAdj(largerUID, weight, nodeList.get(largerUID));

        // Make sure both nodes know the neighbor and the weight
        // Get the smaller UID node, and put the larger UID node as a neighbor
        nodeList.get(smallerUID).NeighborWeights.put(largerUID, nodeAdjLarge);
        // Get the Larger UID node, and put the smaller UID node as a neighbor
        nodeList.get(largerUID).NeighborWeights.put(smallerUID, nodeAdjSmall);

        
        // Set up the neighbors and their weights
        while (true) {
            readLine = b.readLine();
            if(readLine == null){
                break;
            }
            readLine.trim();
            // S[0] = (#,#)
            // S[1] = weight
            s = readLine.split("\\s+");

            weight = Integer.parseInt(s[1]);
            // S[0] smaller UID with (
            // s{1] larger UID with )
            s = s[0].split(",");
            smallerUID = Integer.parseInt(s[0].substring(1));
            largerUID = Integer.parseInt(s[1].substring(0, s[1].length() - 1));
                
            // Create an adj node
            nodeAdjSmall = new NodeAdj(smallerUID, weight, nodeList.get(smallerUID));
            nodeAdjLarge = new NodeAdj(largerUID, weight, nodeList.get(largerUID));

            // Make sure both nodes know the neighbor and the weight
            // Get the smaller UID node, and put the larger UID node as a neighbor
            nodeList.get(smallerUID).NeighborWeights.put(largerUID, nodeAdjLarge);
            // Get the Larger UID node, and put the smaller UID node as a neighbor
            nodeList.get(largerUID).NeighborWeights.put(smallerUID, nodeAdjSmall);   
        }
        
        b.close();
        
        /*
        System.out.println("UID: " + nodeList.get(5).getNodeUID() + " Neigh Size: " + nodeList.get(5).NeighborWeights.size() );
        System.out.println("UID: " + nodeList.get(200).getNodeUID() + " Neigh Size: " + nodeList.get(200).NeighborWeights.size() );
        System.out.println("UID: " + nodeList.get(8).getNodeUID() + " Neigh Size: " + nodeList.get(8).NeighborWeights.size() );
        System.out.println("UID: " + nodeList.get(184).getNodeUID() + " Neigh Size: " + nodeList.get(184).NeighborWeights.size() );
        System.out.println("UID: " + nodeList.get(9).getNodeUID() + " Neigh Size: " + nodeList.get(9).NeighborWeights.size());
        System.out.println("UID: " + nodeList.get(37).getNodeUID() + " Neigh Size: " + nodeList.get(37).NeighborWeights.size());
        System.out.println("UID: " + nodeList.get(78).getNodeUID() + " Neigh Size: " + nodeList.get(78).NeighborWeights.size());*/
    }
}
