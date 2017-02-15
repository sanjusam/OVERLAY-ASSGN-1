package cs455.overlay.node;

import cs455.overlay.wireformats.*;

import java.io.IOException;
import java.net.Socket;

public interface Node {
    void onEvent(final Event event, final Socket socket);
    void processCommand(final String command);
    void initiateNodeRegistration(final String myHostName, final int myPortNum);
    void registerNode(final RegisterRequestEvent registerRequestEventEvent, final Socket socket);
    void deRegisterNode(final DeregisterRequestEvent deregisterRequestEvent, final Socket socket);
    void requestDeRegister(final String hostName, final int portNum);
    void registerNodeAcknowledgement(final RegisterAcknowledgementEvent acknowledgement);
    void setupOverlay(final String command) throws IOException;
    void sendLinkWeight();
    void makeConnectionsOnOverLayNodes(final SendMessagingNodesListEvent sendMessagingNodesListEvent);
    void processLinkWeights(final SendLinkWeightsEvent sendLinkWeightsEvent);
    void listMessagingNodes();
    void listEdgeWeight();
    void initiateMessagingSignalForNodes(final String numRoundsStr);
    void startMessaging(final String numRoundsStr);
    void processReceivedMessage(final TransmitMessageEvent transmitMessageEvent);
    void acknowledgeTaskComplete(final String node, final int port);
    void pullTrafficSummary();
    void printTrafficSummary(final TrafficSummaryEvent trafficSummaryEvent);
    void updateConnectionInfo(final SendListeningPortEvent sendListeningPortEvent, final Socket socket);
    void printShortestPath();
    void exitOverlay();
    void forceExit();

    /*An interface where all the node capabilities are defined.  All the capabilities are defined here, but some is not used in one type for node.
	For example printShortestPath() is a capability for the Messaging node, but doesnt work on Registry.  So Registry's implementation of printShortestPath()
	is just a print message that its not supported.  Where as Messaging node implementation is printing the actual shortest path to all nodes.*/
}
