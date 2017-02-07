package cs455.overlay.transport;

import cs455.overlay.node.Node;
import cs455.overlay.wireformats.EventFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPReceiverThread implements Runnable {

    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final EventFactory eventProcessor = EventFactory.getInstance();


    public TCPReceiverThread(Socket socket, final Node node) throws IOException {
        this.socket = socket;
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        eventProcessor.injectNode(node);
    }

    @Override
    public void run() {
        System.out.println("Initiating Data receiver " + socket.getInetAddress().toString() +"  " + socket.getPort() + " " + socket.getLocalPort());
        while (socket != null) {
            byte[] receivedData = receiveDataByte();
            System.out.println("Received Data on Socket " + socket.getLocalPort());
            eventProcessor.processReceivedEvent(receivedData, socket);
        }
    }

    private synchronized byte[] receiveDataByte() {
        int dataLength;
//        System.out.println("Inside receiveDataByte " );
        try {
            dataLength = dataInputStream.readInt();
//            System.out.println("Data Length Received " + dataLength);
            byte[] data = new byte[dataLength];
            dataInputStream.readFully(data, 0, dataLength);
//            System.out.println("Data Received " + Arrays.toString(data));
            return data;
        } catch (IOException ioe) {
            System.out.println("Error : Failed to receive data.");
            System.out.println(ioe.getMessage());
            return new byte[1];
        }
    }
}
