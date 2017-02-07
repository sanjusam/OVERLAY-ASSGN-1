package cs455.overlay.node;

public class NodeConfiguration {

    final String hostName;
    final int portNum;

    NodeConfiguration(final String hostName, final int portNum) {
        this.hostName = hostName;
        this.portNum = portNum;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPortNum() {
        return portNum;
    }
}
