package cs455.overlay.node;

import java.util.ArrayList;
import java.util.List;

public class NodeDetails {
    private final String nodeName;
    private final int portNum;
    private final List<NodeDetails> myConnections = new ArrayList<>();
    private int allConnections  ; // This keeps tracks of all connection of the node, if it connects, or someone makes a connection.

    NodeDetails(final String nodeName, final int portNum) {
        this.portNum = portNum;
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getPortNum() {
        return portNum;
    }

    public String getFormattedString() {
        return nodeName + ":" + portNum;
    }

    public int getAllConnections() {
        return allConnections;
    }

    public int getNumConnectionsMade() {
        return myConnections.size();
    }

    public int addConnections(final NodeDetails nodeDetails) {
        myConnections.add(nodeDetails);
        ++allConnections;  // Increment my connection as well as the destination nodes connection count.
        nodeDetails.incrementConnectionCount();
        return myConnections.size();
    }

    public List<NodeDetails> getConnections() {
        return myConnections;
    }
    public boolean moreConnectionsAllowed(final int connectionRequested) {
        return allConnections < connectionRequested;
    }

    public void incrementConnectionCount() {
        ++allConnections;
    }

}
