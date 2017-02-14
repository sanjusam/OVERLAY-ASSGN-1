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
            if(receivedData != null) {
                eventProcessor.processReceivedEvent(receivedData, socket);
            } else {
                System.exit(0);  //No point in running the thread, if the socket is closed
            }
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
            System.out.println("INFO : Communication socket closed");
            return  null;
        }
    }
}
