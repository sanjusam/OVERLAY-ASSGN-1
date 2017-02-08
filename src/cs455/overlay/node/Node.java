package cs455.overlay.node;

import cs455.overlay.wireformats.Event;

import java.io.IOException;
import java.net.Socket;

public interface Node {
    void onEvent(final Event event, final Socket socket);  //TODO :: Argument event
    void processCommand(final String command);  // TODO::  Can be taken out
    void setupOverlay(final String command) throws IOException;
    void sendLinkWeight();
    void processLinkWeights();
    void listMessagingNodes();
    void ListEdgeWeight();
}
