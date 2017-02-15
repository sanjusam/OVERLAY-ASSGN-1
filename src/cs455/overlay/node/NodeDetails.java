package cs455.overlay.node;

import cs455.overlay.constants.MessageConstants;

import java.util.ArrayList;
import java.util.List;

public class NodeDetails {
    private final String nodeName;
    private final int portNum;
    private final List<NodeDetails> myConnections = new ArrayList<>();
    private final List<NodeDetails> myRemoteConnections = new ArrayList<>(); // Keep tracks of the nodes that are connected to me.
    private int allConnections = 0 ; // This keeps tracks of all connection of the node, if it connects, or someone makes a connection.

    public NodeDetails(final String nodeName, final int portNum) {
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
        return nodeName + MessageConstants.NODE_PORT_SEPARATOR + portNum;
    }

    public int getAllConnections() {
        return allConnections;
    }

    public int getNumConnectionsMade() {
        return myConnections.size();
    }

    public int addConnections(final NodeDetails nodeDetails) {
        myConnections.add(nodeDetails);
        nodeDetails.addRemoteConnections(this);  //Add this nodes details to the remote connection of the connecting node.
        ++allConnections;  // Increment my connection count as well as the destination nodes connection count.
        nodeDetails.incrementConnectionCount();
        return myConnections.size();
    }

    public int addRemoteConnections(final NodeDetails nodeDetails) {
        myRemoteConnections.add(nodeDetails);
        return myRemoteConnections.size();
    }

    public List<NodeDetails> getRemoteConnections() {
        return myRemoteConnections;
    }

    public List<NodeDetails> getConnections() {
        return myConnections;
    }

    public boolean moreConnectionsAllowed(final int connectionRequirement) {
        return allConnections < connectionRequirement;
    }

    public void incrementConnectionCount() {
        ++allConnections;
    }

    public boolean nodeAlreadyConnected(final NodeDetails nodeDetails) {
        for(final NodeDetails nodeConnections : myConnections) {  // Check if the node is connected directly.  A->B
            if(nodeConnections.getFormattedString().equals(nodeDetails.getFormattedString())) {
                return true;
            }
        }

        for(final NodeDetails nodeConnections : myRemoteConnections) {  // Check if the node is connected directly.  B->A
            if(nodeConnections.getFormattedString().equals(nodeDetails.getFormattedString())) {
                return true;
            }
        }
        return false;
    }

}
