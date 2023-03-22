import java.io.*;
import java.net.*;

//Client Class
public class TCPClient {
    String serverHostName;
    String clientHostName;
    int serverPort;
    int UID;
    int serverUID;
    Socket clientsocket;
    ObjectInputStream in;
    ObjectOutputStream out;
    Node node;

    public TCPClient() {
    }

    public TCPClient(int UID, int serverPortNumber, String serverHostName, String clientHostName, int serverUID, Node node) {
        this.UID = UID;
        this.serverHostName = serverHostName;
        this.serverPort = serverPortNumber;
        this.clientHostName = clientHostName;
        this.serverUID = serverUID;
        this.node = node;
    }

    public void clientListeningSocket() {
        try {
            clientsocket = new Socket(serverHostName, serverPort, InetAddress.getByName(clientHostName), 0);

            out = new ObjectOutputStream(clientsocket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(clientsocket.getInputStream());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recieveMessage() {
        try {
            while (true) {
                Messages messageRecieved = (Messages) in.readObject();
                node.addMessageToQueue(messageRecieved);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Break in TCP Read");
        }
    }

    public void establishConnection() {
        try {
            String msg = Integer.toString(node.getNodeUID());
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            System.out.println("Node: " + node.getNodeUID() + " failed connection with " + serverUID);
            e.printStackTrace();
        }
    }

}