public class NodeAdj {
    int UID;
	int weight;
    Node node;

	//Constructor
	public NodeAdj(int UID, int weight, Node node){
		this.UID= UID;
		this.weight= weight;
        this.node = node;
	}

	//Setter
	public void setUID(){}
	public void setWeight(){}
	public void setNode(){}

	//Getters
	public int getUID(){return this.UID;}
	public int getWeight(){return this.weight;}
	public Node getNode(){return this.node;}
	
}
