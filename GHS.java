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
                            // Send out the test message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(testMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                Thread.sleep(15000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        int testRecieved = 0, ackReceived = 0, nackRecieved = 0, count = 0;
                        ArrayList<Integer> sendNack = new ArrayList<>();
                        while(testRecieved < node.getNeighbors().size() || (ackReceived+nackRecieved) < node.getNeighbors().size()){
                            // check message queue - can recieve TEST, ACK or NACK
                            
                            if (!node.messageQueue.isEmpty()) {
                                Messages messageRecieved = node.getMessage();
                                // Recieves a TEST
                                if (messageRecieved.getTypeOfMessage() == Type.TEST) {
                                    // Check to see if the edge weight from this node is smaller than one already seen
                                    if (node.getNeighbors().get(messageRecieved.UIDofSender).getWeight() < currentLowestWeight) {
                                        currentLowestWeight = node.getNeighbors().get(messageRecieved.UIDofSender).getWeight();
                                        currentLowestUID = messageRecieved.getUIDofSender();
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
                                        // Send out the ack message
                                        node.connectedClients.forEach((clientHandler) -> {
                                            try {
                                                clientHandler.getOutputWriter().writeObject(ackMessage);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        try {
                                            Thread.sleep(15000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        
                                        // Create new nack message - weight is not necessary - intended recipeint shows who the connecting edge will be
                                        Messages nackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.NACK);
                                        // Send out the nack message
                                        node.connectedClients.forEach((clientHandler) -> {
                                            try {
                                                clientHandler.getOutputWriter().writeObject(nackMessage);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        try {
                                            Thread.sleep(15000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                // Recieves an ACK
                                else if (messageRecieved.getTypeOfMessage() == Type.ACK) {
                                    //Check to ensure this node is the intended recipient and the correct MWOE
                                    if(messageRecieved.getIntendedRecipient() == node.getNodeUID() && messageRecieved.getUIDofSender() == currentLowestUID){
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
                                   // System.out.println(node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
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
                    ArrayList<Integer> ackReceivedFromNeighbor = new ArrayList<>();
                    boolean searchSent = false;
                    //While a merge has not occured
                    while(roundNFragmentSize == node.getFragmentSize()){
                        if(!searchSent){
                            searchSent = true;
                            //Leader sends out Search to all neighbors
                            Messages searchMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.SEARCH);
                            // Send out the Search message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(searchMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }            

                            //Send test to all neighbors
                            // Create new test message - weight and intended recipient are not necessary here
                            Messages testMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.TEST);
                            // Send out the test message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(testMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                Thread.sleep(100);
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
                                    //Add candidate to list <UID of node in component with edge, edge weight> if they are not already present
                                    if(!candidates.containsKey(messageRecieved.UIDofSender)){
                                        candidates.put(messageRecieved.UIDofSender, messageRecieved.getWeight());
                                    }
                                   
                                }
                            }
                            //Test
                            else if(messageRecieved.getTypeOfMessage() == Type.TEST){
                                //Check the leader to ensure they dont match
                                if(messageRecieved.getLeaderUID() != node.getNodeUID()){
                                    // Check to see if the edge weight from this node is smaller than one already seen
                                    if (node.getNeighbors().get(messageRecieved.UIDofSender).getWeight() < currentLowestWeight) {
                                        currentLowestWeight = node.getNeighbors().get(messageRecieved.UIDofSender).getWeight();
                                        currentLowestUID = messageRecieved.getUIDofSender();
                                    }
                                    //else a smaller edge already exist
                                }
                                testRecieved++;
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
                                    //Add it to a list - will check this list before merging in case the leader has the MWOE
                                    ackReceivedFromNeighbor.add(messageRecieved.getUIDofSender());
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
                                // Send out the ack message
                                node.connectedClients.forEach((clientHandler) -> {
                                    try {
                                        clientHandler.getOutputWriter().writeObject(ackMessage);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                
                                
                                // Create new nack message - weight is not necessary - intended recipeint shows who the connecting edge will be
                                Messages nackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.NACK);
                                // Send out the nack message
                                node.connectedClients.forEach((clientHandler) -> {
                                    try {
                                        clientHandler.getOutputWriter().writeObject(nackMessage);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                            //Send a merge message to children so they can send their ack/nacks
                            //Create Merge Message with inteded Recipient "Sender of smallest"
                            Messages mergeMessage = new Messages(node.getLeader(), level, node.getNodeUID(), smallestCandidateWeight, smallestCandidateUID, Type.MERGE);
                            // Send out the test message
                            node.connectedClients.forEach((clientHandler) -> {
                                try {
                                    clientHandler.getOutputWriter().writeObject(mergeMessage);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //Handle the merge
                            if(ackReceivedFromNeighbor.contains(currentLowestUID)){
                                //Keep track of the edges used for later output
                                usedEdges.add(currentLowestUID);

                                //New leader will be bigger fragment so allow bigger leader to initate
                                int newLeader = Math.max(node.getLeader(), node.getNeighbors().get(currentLowestUID).getNode().getLeader());
                                int oldLeader = Math.min(node.getLeader(), node.getNeighbors().get(currentLowestUID).getNode().getLeader());
                                    
                                //Merge the two fragments
                                mergeFragments(newLeader, oldLeader, level, level);
                            }

                            //reset these variable
                            currentLowestWeight = 1000;
                            currentLowestUID = -1;
                            searchSent = false;
                            candidates.clear();
                            ackReceivedFromNeighbor.clear();
                        }
                    }
                }
            }
            // Else the node is not the leader
            else {
                int testRecieved = 0;
                boolean searchPassed = false, mergePassed = false, mergeResponsible = false;
                ArrayList<Integer> candidatePassed = new ArrayList<>(), ackRecieved = new ArrayList<>();
                //While a merge has not occured
                while(roundNFragmentSize == node.getFragmentSize()){
                    //Wait for incoming message
                    if (!node.messageQueue.isEmpty()) {
                        Messages messageRecieved = node.getMessage();
                        //Search
                        if(messageRecieved.getTypeOfMessage() == Type.SEARCH){
                            //Verify it is from the leader of the component
                            if(messageRecieved.getLeaderUID() == node.LeaderUID){
                                //Pass along search 
                                if(!searchPassed){
                                    searchPassed = true;

                                    //Pass the Search message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(messageRecieved);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                
                                    //Send Test to all neighbors
                                    // Create new test message - weight and intended recipient are not necessary here
                                    Messages testMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, -1, Type.TEST);
                                    // Send out the test message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(testMessage);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(50);
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
                                }
                                //else a smaller edge already exist
                            }
                            //Else it is the same leader, increment test received size
                            testRecieved++;
                            
                            //Once all test have been recieved send the Candidate back to the leader
                            if(testRecieved == node.getNeighbors().size()){
                                 // Create new test message - intended recipient will hold the UID of the other side of MWOE
                                 Messages candidateMessage = new Messages(node.getLeader(), level, node.getNodeUID(), currentLowestWeight, currentLowestUID, Type.CANDIDATE);
                                 // Send out the test message
                                 node.connectedClients.forEach((clientHandler) -> {
                                     try {
                                         clientHandler.getOutputWriter().writeObject(candidateMessage);
                                     } catch (IOException e) {
                                         e.printStackTrace();
                                     }
                                 });
                                 try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        //Ack
                        else if(messageRecieved.getTypeOfMessage() == Type.ACK){
                            //Verify this node is the recipient
                            if(messageRecieved.intendedRecipient == node.getNodeUID()){
                                ackRecieved.add(messageRecieved.getUIDofSender());
                            }
                        }
                        //Nack
                        else if(messageRecieved.getTypeOfMessage() == Type.NACK){
                        }
                        //Candidate
                        else if(messageRecieved.getTypeOfMessage() == Type.CANDIDATE){
                            //Verify this candidate is from this nodes component
                            if(messageRecieved.getLeaderUID() == node.getLeader()){
                                //See if you have already passed this candidate
                                if(!candidatePassed.contains(messageRecieved.getUIDofSender())){
                                    //Add it to the list
                                    candidatePassed.add(messageRecieved.getUIDofSender());

                                    //Pass along the message
                                    //Pass the Search message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(messageRecieved);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        //Merge
                        else if(messageRecieved.getTypeOfMessage() == Type.MERGE){
                            //Verify this node is the recipient
                            if(messageRecieved.intendedRecipient == node.getNodeUID()){
                                mergeResponsible = true;
                                // Create new ack message - weight is not necessary
                                Messages ackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.ACK);
                                // Send out the ack message
                                node.connectedClients.forEach((clientHandler) -> {
                                    try {
                                        clientHandler.getOutputWriter().writeObject(ackMessage);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                
                                System.out.println();
                                // Create new nack message - weight is not necessary
                                Messages nackMessage = new Messages(node.getLeader(), level, node.getNodeUID(), -1, currentLowestUID, Type.NACK);
                                // Send out the ack message
                                node.connectedClients.forEach((clientHandler) -> {
                                    try {
                                        clientHandler.getOutputWriter().writeObject(nackMessage);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });  
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }  
                                
                                                           
                            }
                            //Else check if the message is for this component
                            else if(messageRecieved.getLeaderUID() == node.getLeader()){
                                //Pass it to all neighbors
                                if(!mergePassed){
                                    mergePassed = true;

                                    //Pass the Search message
                                    node.connectedClients.forEach((clientHandler) -> {
                                        try {
                                            clientHandler.getOutputWriter().writeObject(messageRecieved);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    
                                }

                            }
                            //else it is not for this node/component and may be ignored
                            
                        }
                        //Should not be any other message types to enter here
                        else{
                            System.out.println("ERROR " + node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                        }

                        //If node is ready to merge and responsible for the merge initiate it
                        if(mergeResponsible){
                            //Check to see if all acks are in
                            if(ackRecieved.size() == node.getNeighbors().size()){
                                //Handle the merge
                                //Keep track of the edges used for later output
                                usedEdges.add(currentLowestUID);

                                //New leader will be bigger fragment so allow bigger leader to initate
                                int newLeader = Math.max(node.getLeader(), node.getNeighbors().get(currentLowestUID).getNode().getLeader());
                                int oldLeader = Math.min(node.getLeader(), node.getNeighbors().get(currentLowestUID).getNode().getLeader());
                                    
                                //Merge the two fragments
                                mergeFragments(newLeader, oldLeader, level, level);

                                //Reset variables
                                currentLowestWeight = 1000;
                                currentLowestUID = -1;
                                testRecieved = 0;
                                searchPassed = false;
                                mergePassed = false;
                                mergeResponsible = false;
                                candidatePassed.clear();
                                ackRecieved.clear();
                            }
                        }
                    }
                }
            }
        }
        //Print leader and used edges
        System.out.print(node.getNodeUID() + " has leader " + node.getLeader() + " and used edges: ");
        usedEdges.forEach((node) -> {
            System.out.print(node + " ");
        });
        System.out.println();      
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

        //Wait after merge
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

