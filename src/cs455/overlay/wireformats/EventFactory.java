package cs455.overlay.wireformats;

import cs455.overlay.constants.EventType;
import cs455.overlay.node.Node;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class EventFactory {
    private static EventFactory INSTANCE = new EventFactory();
    private Node node = null;

    public synchronized void injectNode (final Node node) {  //TODO :: Factory Pattern ??
        if(node != null) {
            this.node = node;
        }
    }
    public static synchronized EventFactory getInstance(){
        return INSTANCE;
    }

    public void processReceivedEvent(final byte[] receivedStream, final Socket socket) {
        try {
            final Event event = generateEventFromBytes(receivedStream);
            processEvent(event, socket);
        } catch (final IOException ioe) {
            System.out.println("Exiting : Error in generating Event from Stream.. Exiting");
            ioe.printStackTrace();  //TODO :: Should I exit or keep going??
            System.exit(-1);
        }

    }
    public void processEvent(final Event event, final Socket socket) {
        node.onEvent(event, socket);
    }

    private Event generateEventFromBytes(final byte[] incomingBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingBytes);
        DataInputStream eventBytes = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
        final int eventTypeReceived = eventBytes.readInt();
        if(eventTypeReceived == EventType.REGISTER_REQUEST.getValue()) {
            return new RegisterRequest(incomingBytes);
        } else if (eventTypeReceived == EventType.REGISTER_RESPONSE.getValue()) {
            System.out.println("Node-Registered : Received Register Acknowledgement Event");
            return new RegisterAcknowledgement(incomingBytes);
        } else if (eventTypeReceived == EventType.DEREGISTER_REQUEST.getValue()) {
            System.out.println("Node-Deregister : Received De-register Acknowledgement Event");
            return new DeregisterRequest(incomingBytes);
        } else if (eventTypeReceived == EventType.MESSAGING_NODES_LIST.getValue()) {
            System.out.println("Messaging-Node : Received the list of nodes to be connect");
            return new MessagingNodesList(incomingBytes);
        } else if (eventTypeReceived == EventType.Link_Weights.getValue()) {
            System.out.println("Link Weights : Received Link weights.");
            return new LinkWeights(incomingBytes);
        } else {
            System.out.println("Message Received : Undefined - add proper handling ");
            return new Default();  //TODO ::  Add all cases.
        }
    }
}
