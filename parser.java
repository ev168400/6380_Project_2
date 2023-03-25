import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class parser {
    final static HashMap<Integer, Node> nodeList = new HashMap<>();

    public static Node parseFile(String path, int nodeIdentifier) throws Exception {
        Node node = new Node();
        boolean nodeAssigned = false;
        BufferedReader b = new BufferedReader(new FileReader(path));

        String readLine = "";
        readLine = b.readLine();

        // Ignores any empty lines or commented lines
        while (readLine.isEmpty() || readLine.charAt(0) == '#') {
            readLine = b.readLine();
        }

        // Readline holds the number of nodes in the system
        int numberOfNodes = Integer.parseInt(readLine);
        ArrayList<Integer> orderUID = new ArrayList<>();
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
        Node addNode = new Node(UID, Hostname, Port, NeighborWeight, numberOfNodes);
        nodeList.put(UID, addNode);
        orderUID.add(UID);

        //If the node identifier matches this node then assign it
        if(nodeIdentifier == UID){
            node = addNode;
            nodeAssigned = true;
        }

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
            addNode = new Node(UID, Hostname, Port, NeighborWeight2, numberOfNodes);
            nodeList.put(UID, addNode);
            orderUID.add(UID);

            //If the node identifier matches this node then assign it
            if(nodeIdentifier == UID){
                node = addNode;
                nodeAssigned = true;
            }
    
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
            if (readLine == null) {
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

        //If the node has not yet been assigned then the identifier is the index
        if(!nodeAssigned){
            //Locate the correct UID for the index and assign it
            int findUID = orderUID.get(nodeIdentifier);
            node = nodeList.get(findUID);
        }

        return node;
    }
}
