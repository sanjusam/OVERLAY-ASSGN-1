package cs455.overlay.dijkstra;

import cs455.overlay.constants.MessageConstants;

public class RoutingCache {
    private final String source;
    private final String destination ;
    private  String path ="";
    private  String nextHop ="";

    RoutingCache(final String source, final String destination) {  //Holds the data for each path in context of a node.
        this.source = source;
        this.destination = destination;
    }

    RoutingCache(final String source, final String destination, final String path, final String nextHop) {
        this.source = source;
        this.destination = destination;
        this.path = path;
        this.nextHop = nextHop;
    }

    public void addToPath(final String nextNode) {
        if (nextNode == null) {
            return;
        }
        if(path.isEmpty()) {
            this.path += nextNode;
        } else {
            this.path =  path + MessageConstants.NODE_PATH_SEPARATOR + nextNode;
        }
    }

    public void setNextHop(final String nextHop) {
        this.nextHop = nextHop;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getPath() {
        return path;
    }

    public String getNextHop() {
        return nextHop;
    }
}
