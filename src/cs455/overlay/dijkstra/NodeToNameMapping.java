package cs455.overlay.dijkstra;

import java.util.HashMap;
import java.util.Map;

public class NodeToNameMapping {
    private int nodeNameInNumeric = 0;

    private  final Map<String, Integer> nodeDetailsMap = new HashMap<>();

    public void addNodeMapping(final String nodeDetails) {
        if(nodeDetailsMap.get(nodeDetails) == null) {
            nodeDetailsMap.put(nodeDetails, nodeNameInNumeric);
            ++nodeNameInNumeric;
        }
    }

    public String getNode(final int nodeNum) {
        for(final String nodeDetails : nodeDetailsMap.keySet()) {
            if(nodeDetailsMap.get(nodeDetails) == nodeNum)
                return nodeDetails;
        }
        return null;
    }

    public int getNodeNameInNumber(final String nodeDetail) { // Should be passing in node:portNum format.
        return nodeDetailsMap.get(nodeDetail);
    }

    public Map<String, Integer> getNodeDetails() {
        return nodeDetailsMap;
    }

}
