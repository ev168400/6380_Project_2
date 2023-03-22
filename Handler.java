import java.io.*;
import java.net.*;

class Handler implements Runnable {
	private Socket client;
	ObjectInputStream in;
	ObjectOutputStream out;
	Node dsNode;
	boolean isChild = true;

	public Handler(Socket client, Node dsNode) {
		this.client = client;
		this.dsNode = dsNode;
	}

	// getters
	public ObjectInputStream getInputReader() {return in;}
	public ObjectOutputStream getOutputWriter() {return out;}

	public int getSocket() {return client.getLocalPort();}

	public void run() {
		try {
			in = new ObjectInputStream(client.getInputStream());
			out = new ObjectOutputStream(client.getOutputStream());
			out.flush();
		} catch (IOException e) {
			System.out.println("in or out failed");
			System.exit(-1);
		}

		while (true) {
			try {
				// Read data from client
				// InitialHandShake read
				Object msg = in.readObject();
				if (msg instanceof String) {
					System.out.println(dsNode.getNodeUID() + " established connection with " + msg);
				} else if (msg instanceof Messages) {
					Messages broadcastMessage = (Messages) in.readObject();
					// add received messages to Blocking queue
					this.dsNode.addMessageToQueue(broadcastMessage);

				}

			} catch (IOException | ClassNotFoundException e) {
				System.out.println("Read failed");
				System.exit(-1);
			}
		}
	}
}