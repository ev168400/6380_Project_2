public class GHS {
    Node node;
    int level;

    //Default Constructor
    public GHS(){}

    //Constructor
    public GHS(Node node){
        this.node=node;
        level = 0;
    }

    public Node getNode(){return this.node;}

    public synchronized void startGHS(){
        //Arraylist to hold edges that are in the component

        //While(!done)
            //If leader UID == own UID
                //while phase == 0
                    //If !RequestSent to MWOE
                        //Send a test to MWOE
                        //Device a tie breaker for matching edge weights
                    //Else wait for Ack or Nack or test
                        //Check message queue
                        //If test
                            //if it is along the MWOE
                                //Send ack
                            //If it is not
                                //send nack
                        //If Ack
                            //Merge
                                //New Leader UID will be chosen here
                            //if level < other level
                                //level = other level++
                            //else level > other level
                                //level++
                        //If Nack
                            //start over
                //Else phase > 0
                    //Send search
                    //Wait for response from neighbors
                    //If Test
                        //Send test
                    //If Candidate
            //Else the node is not the leader
                //Check message queue
                //If search
                    //Send a test to MWOE neighbor
                        //Test should include own leader UID   
                //If test
                    //If leader UID != test leaderUID
                        //If it is the MWOE
                            //Send ack
                        //Else it is not the MWOE
                            //Send nack
                    //Else they are from same component
                        //send nack
                        //Add edge to same component list
                //If ack 
                    //Send edge weight back to leader (?how?)
                //If nack
                     //Send -1 back to leader 
    }
}


