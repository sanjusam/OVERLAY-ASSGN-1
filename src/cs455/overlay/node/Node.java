package cs455.overlay.node;

import cs455.overlay.wireformats.*;

import java.io.IOException;
import java.net.Socket;

public interface Node {
    void onEvent(final Event event, final Socket socket);  //TODO :: Argument event
    void processCommand(final String command);  // TODO::  Can be taken out
    void registerNode(final RegisterRequest registerRequestEvent, final Socket socket);
    void deRegisterNode(final DeregisterRequest deregisterRequest, final Socket socket);
    void registerNodeAcknowledgement(final RegisterAcknowledgement acknowledgement);
    void setupOverlay(final String command) throws IOException;
    void sendLinkWeight();
    void processLinkWeights(final LinkWeights linkWeights);
    void listMessagingNodes();
    void listEdgeWeight();
    void initiateMessagingSignalForNodes(final String numRoundsStr);
    void startMessaging(final String numRoundsStr);
    void acknowledgeTaskComplete(final String node, final int port);
    void pullTrafficSummary();
    void printTrafficSummary(final TrafficSummary trafficSummary);
    void printShortestPath();
    void exitOverlay();
}
