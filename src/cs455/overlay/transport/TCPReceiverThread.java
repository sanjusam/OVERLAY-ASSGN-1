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
        while (socket != null) {
            byte[] receivedData = receiveDataByte();
            eventProcessor.processReceivedEvent(receivedData, socket);
        }
    }

    private synchronized byte[] receiveDataByte() {
        int dataLength;
        try {
            dataLength = dataInputStream.readInt();
            byte[] data = new byte[dataLength];
            dataInputStream.readFully(data, 0, dataLength);
            return data;
        } catch (IOException ioe) {
            System.out.println("Error : Failed to receive data.");
            System.out.println(ioe.getMessage());
            return new byte[1];
        }
    }
}
