package cs455.overlay.node;

public class NodeDetails {
    private final String nodeName;
    private final int portNum;
    private int numConnections;

    NodeDetails(final String nodeName, final int portNum) {
        this.portNum = portNum;
        this.nodeName = nodeName;
        numConnections = 0;
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

    public int getNumConnections() {
        return numConnections;
    }

    public int incrementConnections() {
        return  ++numConnections;
    }

}
