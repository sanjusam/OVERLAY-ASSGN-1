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

    public synchronized void injectNode (final Node node) {
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
            ioe.printStackTrace();
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
            return new RegisterRequestEvent(incomingBytes);
        } else if (eventTypeReceived == EventType.REGISTER_RESPONSE.getValue()) {
            System.out.println("Node-Registered : Received Register Acknowledgement Event");
            return new RegisterAcknowledgementEvent(incomingBytes);
        } else if (eventTypeReceived == EventType.DEREGISTER_REQUEST.getValue()) {
            System.out.println("Node-Deregister : Received De-register Acknowledgement Event");
            return new DeregisterRequestEvent(incomingBytes);
        } else if (eventTypeReceived == EventType.MESSAGING_NODES_LIST.getValue()) {
            System.out.println("Messaging-Node : Received the list of nodes to be connect");
            return new SendMessagingNodesListEvent(incomingBytes);
        } else if (eventTypeReceived == EventType.Link_Weights.getValue()) {
            System.out.println("Link Weights : Received Link weights.");
            return new SendLinkWeightsEvent(incomingBytes);
        } else if (eventTypeReceived == EventType.SIGNAL_TO_START_MSG.getValue()) {
            System.out.println("Starting to message");
            return new StartMessagingEvent(incomingBytes);
        }else if (eventTypeReceived ==  EventType.TASK_INITIATE.getValue()) {
            System.out.println("Send the start Message");
            return new TaskInitiateEvent(incomingBytes);
        } else if (eventTypeReceived ==  EventType.TASK_COMPLETE.getValue()) {
                return new TaskCompleteEvent(incomingBytes);
        } else if (eventTypeReceived ==  EventType.PULL_TRAFFIC_SUMMARY.getValue()) {
            return new PullTrafficSummaryEvent(incomingBytes);
        } else if (eventTypeReceived ==  EventType.TRAFFIC_SUMMARY.getValue()) {
            return new TrafficSummaryEvent(incomingBytes);
        } else if (eventTypeReceived ==  EventType.MESSAGE_TRANSMIT.getValue()) {
            return new TransmitMessageEvent(incomingBytes);
        } else if (eventTypeReceived ==  EventType.SEND_LISTENING_PORT.getValue()) {
            return new SendListeningPortEvent(incomingBytes);
        } else if (eventTypeReceived ==  EventType.FORCE_EXIT_EVERYONE.getValue()) {
            return new ForceExitEvent(incomingBytes);
        } else {
            System.out.println("Message Received : Undefined - add proper handling  " + eventTypeReceived);
            return new DefaultEvent();
        }
    }
}
