import java.io.*;
import java.util.*;

public class GHS {
    Node node;
    int level;

    // Default Constructor
    public GHS() {
    }

    // Constructor
    public GHS(Node node) {
        this.node = node;
        level = 0;
    }

    public Node getNode() {
        return this.node;
    }

    public synchronized void startGHS() {
        // Arraylist to hold edges that were used in the tree
        ArrayList<Integer> usedEdges = new ArrayList<>();

        // While there are still outoging edges
        while (node.fragmentSize < node.getNumberNodes()) {
            // Boolean variable for leader to start
            Boolean testSent = false;
            int currentLowestWeight = 1000, currentLowestUID = -1, roundNFragmentSize = node.getFragmentSize();
            // If the node is the leader
            if (node.getNodeUID() == node.getLeader()) {
                if(level == 0){
                    while (level == 0) {
                        // If this is the first pass, let the leader send out a test to all its neighbors
                        if (!testSent) {
                            testSent = true;
                            // Create new test message - weight and intended recipient are not necessary here
                            Messages testMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.TEST);
                            System.out.println(node.getNodeUID() + " sent test to all neighbors");
                            // Send out the test message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(testMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                    Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        int testRecieved = 0, ackReceived = 0, nackRecieved = 0;
                        ArrayList<Integer> sendNack = new ArrayList<>();
                        while(testRecieved < node.getNeighbors().size() || (ackReceived+nackRecieved) < node.getNeighbors().size()){
                            // check message queue - can recieve TEST, ACK or NACK
                            if (!node.messageQueue.isEmpty()) {
                                Messages messageRecieved = node.getMessage();
                                System.out.println(node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                                // Recieves a TEST
                                if (messageRecieved.getTypeOfMessage() == Type.TEST) {
                                    // Check to see if the edge weight from this node is smaller than one already seen
                                    if (node.getNeighbors().get(messageRecieved.UIDofSender).getWeight() < currentLowestWeight) {
                                        currentLowestWeight = node.getNeighbors().get(messageRecieved.UIDofSender).getWeight();
                                        currentLowestUID = messageRecieved.getUIDofSender();
                                        System.out.println("Current lowest weight: " +  currentLowestWeight + " : " + currentLowestUID);
                                    }
                                    //If it isnt add the uid to a list of rejected edges
                                    else{
                                        sendNack.add(messageRecieved.UIDofSender);
                                    }
                                    //If it isnt continue waiting for all test
                                    testRecieved++;

                                    //Once all test come in then send your own ack and nack
                                    if(testRecieved == node.getNeighbors().size()){
                                        // Create new ack message - weight is not necessary
                                        Messages ackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.ACK);
                                        System.out.println(node.getNodeUID() + " sent ack to all neighbors with intended " + currentLowestUID);
                                        // Send out the ack message
                                        node.connectedClients.forEach((clientHandler) -> {
                                            try {
                                                clientHandler.getOutputWriter().writeObject(ackMessage);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        try {
                                            Thread.sleep(10000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        // Create new nack message - weight is not necessary - intended recipeint shows who the connecting edge will be
                                        Messages nackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.NACK);
                                        System.out.println(node.getNodeUID() + " sent nack to all neighbors with intended " + currentLowestUID);
                                        // Send out the nack message
                                        node.connectedClients.forEach((clientHandler) -> {
                                            try {
                                                clientHandler.getOutputWriter().writeObject(nackMessage);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        try {
                                            Thread.sleep(10000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                // Recieves an ACK
                                else if (messageRecieved.getTypeOfMessage() == Type.ACK) {
                                    //Check to ensure this node is the intended recipient and the correct MWOE
                                    if(messageRecieved.getIntendedRecipient() == node.getNodeUID() && messageRecieved.getUIDofSender() == currentLowestUID){
                                        System.out.println(node.getNodeUID() + " is ready to merge with " + messageRecieved.UIDofSender);
                                        ackReceived++;

                                        //Keep track of the edges used for later output
                                        usedEdges.add(messageRecieved.getUIDofSender());

                                        //New leader will be bigger fragment so allow bigger leader to initate
                                        int newLeader = Math.max(node.getLeader(), messageRecieved.getLeaderUID());
                                        int oldLeader = Math.min(node.getLeader(), messageRecieved.getLeaderUID());
                                        
                                        //Merge the two fragments
                                        mergeFragments(newLeader, oldLeader, level, messageRecieved.getPhase());

                                        //reset these variables
                                        currentLowestUID = -1;
                                        currentLowestWeight = 1000;

                                    }
                                    //else this message was meant for another node, or the MWOE was not reciprocated
                                    else if(messageRecieved.getIntendedRecipient() == node.getNodeUID() && messageRecieved.getUIDofSender() != currentLowestUID){
                                        nackRecieved++;
                                    }

                                }
                                // Recieves a Nack
                                else if (messageRecieved.getTypeOfMessage() == Type.NACK) {
                                    //Intended Recipient will hold the node chosen for the ack, if that is not this node then the nack is meant for this node
                                    if(messageRecieved.getIntendedRecipient() != node.getNodeUID()){
                                        nackRecieved++;
                                    }

                                }
                                // Any other message type may be ignored while in phase 0
                                else {
                                    System.out.println(node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                                }
                            }
                            testSent = false;
                        }
                    }
                }
                //If phase 0 has already occured
                else if(level > 0){
                    //List to hold all candidates
                    HashMap<Integer, Integer> candidates = new HashMap<>();
                    int testRecieved = 0;
                    boolean searchSent = false;
                    //While a merge has not occured
                    while(roundNFragmentSize == node.getFragmentSize()){
                        if(!searchSent){
                            searchSent = true;
                            //Leader sends out Search to all neighbors
                            Messages searchMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.SEARCH);
                            System.out.println(node.getNodeUID() + " sent search to all neighbors");
                            // Send out the Search message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(searchMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //Send test to all neighbors
                            // Create new test message - weight and intended recipient are not necessary here
                            Messages testMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.TEST);
                            System.out.println(node.getNodeUID() + " sent test to all neighbors");
                            // Send out the test message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(testMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                    Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        //Waits for messages
                        if (!node.messageQueue.isEmpty()) {
                            Messages messageRecieved = node.getMessage();
                            //Candidate
                            if(messageRecieved.getTypeOfMessage() == Type.CANDIDATE){
                                //Verify this candidate is from this component
                                if(messageRecieved.getLeaderUID() == node.getNodeUID()){
                                    //Add candidate to list <UID of node in component with edge, edge weight>
                                    candidates.put(messageRecieved.UIDofSender, messageRecieved.getWeight());
                                    //Once you have recieved all candidates
                                    if(candidates.size() == node.getFragmentSize()){
                                        int smallestCandidateUID = 0, smallestCandidateWeight = 1000;
                                        //Find the smallest 
                                        for(Map.Entry<Integer,Integer> entry: candidates.entrySet()){
                                            if(smallestCandidateWeight > entry.getValue()){
                                                smallestCandidateWeight = entry.getValue();
                                                smallestCandidateUID = entry.getKey();
                                            }
                                        }
                                        //If the leader has the MWOE
                                        if(smallestCandidateUID == node.getNodeUID()){
                                            // Create new ack message - weight is not necessary
                                            Messages ackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.ACK);
                                            System.out.println(node.getNodeUID() + " sent ack to all neighbors with intended " + currentLowestUID);
                                            // Send out the ack message
                                            node.connectedClients.forEach((clientHandler) -> {
                                                try {
                                                    clientHandler.getOutputWriter().writeObject(ackMessage);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                            try {
                                                Thread.sleep(10000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            // Create new nack message - weight is not necessary - intended recipeint shows who the connecting edge will be
                                            Messages nackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.NACK);
                                            System.out.println(node.getNodeUID() + " sent nack to all neighbors with intended " + currentLowestUID);
                                            // Send out the nack message
                                            node.connectedClients.forEach((clientHandler) -> {
                                                try {
                                                    clientHandler.getOutputWriter().writeObject(nackMessage);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                            try {
                                                Thread.sleep(10000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                        //Else a child has the MWOE
                                        else{
                                            //Create Merge Message with inteded Recipient "Sender of smallest"
                                            Messages mergeMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.MERGE);
                                            System.out.println(node.getNodeUID() + " sent merge to all neighbors with intended recipient " + currentLowestUID);
                                            // Send out the test message
                                            node.connectedClients.forEach((clientHandler) -> {
                                                try {
                                                    clientHandler.getOutputWriter().writeObject(mergeMessage);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                            try {
                                                    Thread.sleep(10000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        //reset these variable
                                        currentLowestWeight = 1000;
                                        currentLowestUID = -1;
                                        searchSent = false;
                                        try {
                                            Thread.sleep(10000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                //Else it is from a different component and may be ignored
                            }
                            //Test
                            else if(messageRecieved.getTypeOfMessage() == Type.TEST){
                                //Check the leader to ensure they dont match
                                if(messageRecieved.getLeaderUID() != node.getNodeUID()){
                                    // Check to see if the edge weight from this node is smaller than one already seen
                                    if (node.getNeighbors().get(messageRecieved.UIDofSender).getWeight() < currentLowestWeight) {
                                        currentLowestWeight = node.getNeighbors().get(messageRecieved.UIDofSender).getWeight();
                                        currentLowestUID = messageRecieved.getUIDofSender();
                                        System.out.println("Current lowest weight: " +  currentLowestWeight + " : " + currentLowestUID);
                                    }
                                    //else a smaller edge already exist
                                    testRecieved++;
                                }
                                //Once all test have been recieved add the smallest to the list of candidates
                                if(testRecieved == node.getNeighbors().size()){
                                    candidates.put(node.getNodeUID(), currentLowestWeight);
                                }

                            }
                            //Search
                            else if(messageRecieved.getTypeOfMessage() == Type.SEARCH){
                                //Since this is the leader node any Search is either passed from a child or different component and may be ignored
                            }
                            //Ack
                            else if(messageRecieved.getTypeOfMessage() == Type.ACK){
                                //Verify who the ack is intended for
                                if(messageRecieved.getIntendedRecipient() == node.getNodeUID()){
                                    //If this node is the intended recipient verify it is reciprocated
                                    if(messageRecieved.getUIDofSender() == currentLowestUID){
                                        //If it is reciprocated merge the components
                                        System.out.println(node.getNodeUID() + " is ready to merge with " + messageRecieved.UIDofSender);

                                        //Keep track of the edges used for later output
                                        usedEdges.add(messageRecieved.getUIDofSender());

                                        //New leader will be bigger fragment so allow bigger leader to initate
                                        int newLeader = Math.max(node.getLeader(), messageRecieved.getLeaderUID());
                                        int oldLeader = Math.min(node.getLeader(), messageRecieved.getLeaderUID());
                                            
                                        //Merge the two fragments
                                        mergeFragments(newLeader, oldLeader, level, messageRecieved.getPhase());

                                        //reset these variables
                                        currentLowestUID = -1;
                                        currentLowestWeight = 1000;
                                    }//Else it was not reciprocated
                                }
                            }
                            //Nack
                            else if(messageRecieved.getTypeOfMessage() == Type.NACK){
                            }
                            //Merge
                            else if(messageRecieved.getTypeOfMessage() == Type.MERGE){
                                //Since this is the leader then Merge is from different component or child and may be ignored
                            }
                            //Should not be any other message types to enter here
                            else{
                                System.out.println("ERROR " + node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                            }
                        }
                    }
                }
            }
            // Else the node is not the leader
            else {
                int testRecieved = 0;
                boolean searchPassed = false;
                //While a merge has not occured
                while(roundNFragmentSize == node.getFragmentSize()){
                    //Wait for incoming message
                    if (!node.messageQueue.isEmpty()) {
                        Messages messageRecieved = node.getMessage();
                        System.out.println(node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                        //Search
                        if(messageRecieved.getTypeOfMessage() == Type.SEARCH){
                            //Verify it is from the leader of the component
                            if(messageRecieved.getLeaderUID() == node.LeaderUID){
                                //Pass along search 
                                if(!searchPassed){
                                    searchPassed = true;

                                    System.out.println(node.getNodeUID() + " passed search to all neighbors");
                                    //Pass the Search message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(messageRecieved);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                
                                    //Send Test to all neighbors
                                    // Create new test message - weight and intended recipient are not necessary here
                                    Messages testMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.TEST);
                                    System.out.println(node.getNodeUID() + " sent test to all neighbors");
                                    // Send out the test message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(testMessage);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                            Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            //Else it is not from this nodes leader and may be ignored
                        }
                        //Test
                        else if(messageRecieved.getTypeOfMessage() == Type.TEST){
                            //Check the leader to ensure they dont match
                            if(messageRecieved.getLeaderUID() != node.getLeader()){
                                // Check to see if the edge weight from this node is smaller than one already seen
                                if (node.getNeighbors().get(messageRecieved.UIDofSender).getWeight() < currentLowestWeight) {
                                    currentLowestWeight = node.getNeighbors().get(messageRecieved.UIDofSender).getWeight();
                                    currentLowestUID = messageRecieved.getUIDofSender();
                                    System.out.println("Current lowest weight: " +  currentLowestWeight + " : " + currentLowestUID);
                                }
                                //else a smaller edge already exist
                            }
                            //Else it is the same leader, increment test received size
                            testRecieved++;
                            
                            //Once all test have been recieved send the Candidate back to the leader
                            if(testRecieved == node.getNeighbors().size()){
                                // Create new test message - intended recipient will hold the UID of the other side of MWOE
                                Messages candidateMessage = new Messages(node.getLeader(), level, node.getNodeUID(), currentLowestWeight, currentLowestUID, Type.CANDIDATE);
                                System.out.println(node.getNodeUID() + " sent candidate" + currentLowestUID + " of weight " + currentLowestWeight + " to all neighbors");
                                // Send out the test message
                                node.connectedClients.forEach((clientHandler) -> {
                                    try {
                                        clientHandler.getOutputWriter().writeObject(candidateMessage);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                try {
                                        Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        //Ack
                        else if(messageRecieved.getTypeOfMessage() == Type.ACK){
                            //Verify who the ack is intended for
                            if(messageRecieved.getIntendedRecipient() == node.getNodeUID()){
                                //If this node is the intended recipient verify it is reciprocated
                                if(messageRecieved.getUIDofSender() == currentLowestUID){
                                    //If it is reciprocated merge the components
                                    System.out.println(node.getNodeUID() + " is ready to merge with " + messageRecieved.UIDofSender);

                                    //Keep track of the edges used for later output
                                    usedEdges.add(messageRecieved.getUIDofSender());

                                    //New leader will be bigger fragment so allow bigger leader to initate
                                    int newLeader = Math.max(node.getLeader(), messageRecieved.getLeaderUID());
                                    int oldLeader = Math.min(node.getLeader(), messageRecieved.getLeaderUID());
                                        
                                    //Merge the two fragments
                                    mergeFragments(newLeader, oldLeader, level, messageRecieved.getPhase());

                                    //reset these variables
                                    currentLowestUID = -1;
                                    currentLowestWeight = 1000;
                                }
                            }//Else the ack is not meant for this node
                                
                        }
                        //Nack
                        else if(messageRecieved.getTypeOfMessage() == Type.NACK){
                        }
                        //Candidate
                        else if(messageRecieved.getTypeOfMessage() == Type.CANDIDATE){
                            //Verify it is coming from same component 
                            if(messageRecieved.getLeaderUID() == node.getLeader()){
                                //Pass along Candidate 
                                System.out.println(node.getNodeUID() + " passed Candidate to all neighbors");
                                // Send out the candidate message
                                node.connectedClients.forEach((clientHandler) -> {
                                    try {
                                        clientHandler.getOutputWriter().writeObject(messageRecieved);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                            //Else it is from a different component and may be ignored
                        }
                        //Merge
                        else if(messageRecieved.getTypeOfMessage() == Type.MERGE){
                            //Verify it is from this nodes leader
                            if(messageRecieved.getLeaderUID() == node.getLeader()){
                                //Verify it is intended for this node
                                if(messageRecieved.getIntendedRecipient() == node.nodeUID){
                                    // Create new ack message - weight is not necessary
                                    Messages ackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.ACK);
                                    System.out.println(node.getNodeUID() + " sent ack to all neighbors with intended " + currentLowestUID);
                                    // Send out the ack message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(ackMessage);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    // Create new nack message - weight is not necessary - intended recipeint shows who the connecting edge will be
                                    Messages nackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.NACK);
                                    System.out.println(node.getNodeUID() + " sent nack to all neighbors with intended " + currentLowestUID);
                                    // Send out the nack message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(nackMessage);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }
                                //Else it is meant for another node in the same component
                                else{
                                    //Pass along Merge 
                                    System.out.println(node.getNodeUID() + " passed Merge to all neighbors");
                                    // Send out the Merge message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(messageRecieved);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        //Should not be any other message types to enter here
                        else{
                            System.out.println("ERROR " + node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                        }
                    }
                }
            }
            System.out.println("Ready to move to next phase");
            System.out.println("Level: " + level + " Fragment size: " + node.getFragmentSize());
            usedEdges.forEach((node) -> {
                System.out.print(node + " ");
            });
            System.out.println();
        }      
    }

    //Function to merge two fragments
    public void mergeFragments(int newLeader, int oldLeader, int levelOne, int levelTwo){
        //The level is incremented by one if they are the same
        if(levelOne == levelTwo){
            level++;
        }
        //If one is smaller than it is absorbed by two
        else if(levelOne < levelTwo){
            level = levelTwo;
        }
        //Else two is absorbed by one
        else{
            level = levelOne;
        }

        //The new fragment size is a combination of the two before
        int fragmentSize = parser.nodeList.get(newLeader).getFragmentSize() + parser.nodeList.get(oldLeader).getFragmentSize();

        //Updates the leader to be the bigger of the previous two
        for (Map.Entry<Integer, Node> node : parser.nodeList.entrySet()) {
            //If it is from the old leader, change the leader and the fragment size
            if(node.getValue().getLeader() == oldLeader){
                node.getValue().setLeader(newLeader);
                node.getValue().setFragmentSize(fragmentSize);
            }//If it is from the newLeader component only change the fragment size
            else if(node.getValue().getLeader() == newLeader){
                node.getValue().setFragmentSize(fragmentSize);
            }
        }
    }
}

