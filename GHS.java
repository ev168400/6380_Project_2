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
        // Arraylist to hold edges that are in the component
        ArrayList<Integer> sameComponent = new ArrayList<>();

        // Arraylist to hold edges that were used in the tree
        ArrayList<Integer> usedIntegers = new ArrayList<>();

        // Boolean variable for leader to start
        Boolean testSent = false;
        int currentLowestWeight = 1000, currentLowestUID = -1;

        // While there are still outoging edges
        while (sameComponent.size() < node.getNeighbors().size()) {
            // If the node is the leader
            if (node.getNodeUID() == node.getLeader()) {
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
                                    //Merge the two fragments
                                    System.out.println(node.getNodeUID() + " is ready to merge with " + messageRecieved.UIDofSender);
                                    ackReceived++;

                                    //increment the level

                                }
                                //else this message was meant for another node, or the MWOE was not reciprocated
                                else{
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
                            // Any other message type should not be sent in this phase
                            else {
                                System.out.println(node.getNodeUID() + " recieved " + messageRecieved.getTypeOfMessage() + " from " + messageRecieved.getUIDofSender());
                            }

                        }
                    }
                    System.out.println("Ready to move to next phase");
                    System.out.println(ackReceived + " " + nackRecieved);
                }
            }
            // Else the node is not the leader
            else {

            }
        }      
    }
}

  // While(!done)
            // If leader UID == own UID
                // while phase == 0
                    // If !RequestSent
                        // Send a test to all neighbors
                        // Device a tie breaker for matching edge weights
                    // Else wait for Ack or Nack or test
                        // Check message queue
                        // If test
                            // if it is along the MWOE
                                // Send ack
                            // If it is not
                                // send nack
                        // If Ack
                            // Merge
                            // New Leader UID will be chosen here
                            // if level < other level
                            // level = other level++
                            // else level > other level
                            // level++
                        // If Nack
                        // start over
                // Else phase > 0
                    // Send search
                        // Wait for response from all neighbors
                    // If Test
                        // Send test
                    // If Candidate
            // Else the node is not the leader
                // Check message queue
                // If search
                // Send a test to all neighbor
                // Test should include own leader UID
                // If test
                // If leader UID != test leaderUID
                // If it is the MWOE
                // Send ack
                // Else it is not the MWOE
                // Send nack
                // Else they are from same component
                // send nack
                // Add edge to same component list
                // If ack
                // Send edge weight back to leader (?how?)
                // If nack
                // Send -1 back to leader
