package cs455.overlay.node;

public class NodeDetails {
    final String nodeName;
    final int portNum;

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
}
