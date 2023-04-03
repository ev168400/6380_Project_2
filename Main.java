import java.net.*;
import java.util.*;
import java.lang.*;

public class Main {
    public static void main(String[] args) {
        String clientHostName = "";
        
		try {
			clientHostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
        Node mainNode = BuildNode(args[0], Integer.parseInt(args[1]));

		
        System.out.println("Starting node: " + mainNode.nodeUID);
		Runnable serverRunnable = new Runnable() {
			public void run() {
				TCPServer server = new TCPServer(mainNode);
				// start listening for client requests
				server.listenSocket();
			}
		};
		Thread serverthread = new Thread(serverRunnable);
		serverthread.start();

        for (Map.Entry<Integer, Node> node : parser.nodeList.entrySet()) {
            if(node.getValue().getNodeHostName().equals(clientHostName) && node.getValue().getNodeUID() == mainNode.getNodeUID()){
                node.getValue().NeighborWeights.entrySet().forEach((neighbor) -> {
                    Runnable clientRunnable = new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            TCPClient client = new TCPClient(node.getValue().getNodeUID(),
                                    neighbor.getValue().getNode().getNodeListeningPort(),
                                    neighbor.getValue().getNode().getNodeHostName(), node.getValue().hostName, neighbor.getValue().getNode().getNodeUID(), mainNode);
                            client.clientListeningSocket();
                            client.establishConnection();
                            client.recieveMessage();
                        }
                    };
                    Thread clientthread = new Thread(clientRunnable);
					clientthread.start();
                });
                break;
            }
        }
        try {
            while (mainNode.connectedClients.size() < mainNode.getNeighbors().size()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Start algorithm
        System.out.println(mainNode.getNodeUID() + " is initiating GHS");
        GHS algorithm = new GHS(mainNode);
        algorithm.startGHS();

    }
    
    public static Node BuildNode(String path, int nodeIdentifier) {
		Node mainNode = new Node();
		try {
			mainNode = parser.parseFile(path, nodeIdentifier);
		} catch (Exception e) {
			throw new RuntimeException("Unable to get nodeList", e);
		}
		return mainNode;
	}
}
