import java.io.IOException;
import java.net.*;

//Server Class
public class TCPServer {
	ServerSocket serverSocket;
	String hostName;
	int portNumber;
	Node node;
	int UID;

	public TCPServer() {}

	public TCPServer(String hostName, int portNumber, int UID) {
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.UID = UID;
	}

	public TCPServer(Node dsNode) {
		this.node = dsNode;
		this.UID = dsNode.getNodeUID();
		this.portNumber = dsNode.getNodeListeningPort();
		this.hostName = dsNode.getNodeHostName();
	}

	public void listenSocket() {
		try {
			serverSocket = new ServerSocket(portNumber);
			System.out.println("ServerSocket open with port: " + portNumber);
		} catch (IOException e) {
			System.out.println("Could not listen on port " + portNumber);
			System.out.println(e);
			System.exit(-1);
		}
		while (true) {
			Handler reqHandler;
			try {
				// server.accept returns a client connection
				Socket clientreqSocket = serverSocket.accept();
				reqHandler = new Handler(clientreqSocket, this.node);

				// add all the connected clients
				node.addClient(reqHandler);

				// assign each client request to a separate thread
				Thread t = new Thread(reqHandler);
				t.start();

			} catch (IOException e) {
				System.out.println("Accept failed");
				e.printStackTrace();
			}
		}
	}
}
