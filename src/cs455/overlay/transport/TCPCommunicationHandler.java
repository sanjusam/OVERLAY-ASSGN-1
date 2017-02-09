package cs455.overlay.transport;

import cs455.overlay.node.Node;

import java.io.IOException;
import java.net.Socket;

public class TCPCommunicationHandler {
    private TCPSender sender;
    private TCPReceiverThread receiver;

    public TCPCommunicationHandler(final Socket socket, final Node node) {
        try {
            sender = new TCPSender(socket);
            receiver = new TCPReceiverThread(socket, node);
            final Thread receiverThread = new Thread(receiver);
            receiverThread.start();
        } catch (final IOException ioe) {
            System.out.println("Exiting : Unable to initialize TCPCommunication handler for " + socket.getInetAddress().toString());
            System.exit(-1);
        }
    }

    public boolean sendData(byte[] dataToSend) {
        try {
            sender.sendData(dataToSend);
            return true;
        }catch (IOException ioe) {
            System.out.println("Exiting : Unable to send data");
            ioe.printStackTrace();
            return  false;
        }
    }

}
