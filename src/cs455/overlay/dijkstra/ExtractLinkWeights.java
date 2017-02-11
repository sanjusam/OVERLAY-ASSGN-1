package cs455.overlay.dijkstra;

import cs455.overlay.constants.MessageConstants;
import cs455.overlay.utils.HelperUtils;

import java.util.ArrayList;
import java.util.List;

public class ExtractLinkWeights {
    private List<String> linkWeightList = new ArrayList<>();  // hostnameA:portnumA hostnameB:portnumB weight
    private final NodeToNameMapping nodeNameMapping = new NodeToNameMapping();
    private final List<RoutingCache> routingCacheList = new ArrayList<>();

    public ExtractLinkWeights(final List<String> linkWeightList, final String me) {
        this.linkWeightList = linkWeightList;
        extractNodes();
        final int numExpressionOfMe = nodeNameMapping.getNodeNameInNumber(me);
        final ShortestPathGenerator shortestPathGenerator = new ShortestPathGenerator(linkWeightList.size(), numExpressionOfMe);
        sendEdgeWeightsForCalculation(shortestPathGenerator);
        generatePathsForTraffic(shortestPathGenerator);
    }

    private void sendEdgeWeightsForCalculation(final ShortestPathGenerator generator) {
        for(final String linkWeight : linkWeightList) {
            final String [] nodeParts = linkWeight.split(" ");
            int sourceLink = nodeNameMapping.getNodeNameInNumber(nodeParts[0].trim());
            int destLink = nodeNameMapping.getNodeNameInNumber(nodeParts[1].trim());
            int edgeWeight = HelperUtils.getInt(nodeParts[2].trim());
            generator.addEdge(sourceLink, destLink, edgeWeight);
        }
    }

    private List<RoutingCache> generatePathsForTraffic(final ShortestPathGenerator generator) {
        generator.buildPaths();
        final List<RoutingCache> routingCacheListTemp = generator.generatePathForTraffic();
        for (final RoutingCache routingCacheTemp : routingCacheListTemp) {
            String newSource = getActualNode(routingCacheTemp.getSource());
            String newDest = getActualNode(routingCacheTemp.getDestination());
            String route = getActualNode(routingCacheTemp.getPath());
            String nextHop = getActualNode(routingCacheTemp.getNextHop());
            RoutingCache actualRoutingCache = new RoutingCache(newSource, newDest, route, nextHop);
            routingCacheList.add(actualRoutingCache);
        }
        return routingCacheList;
    }


    private String getActualNode(final String  listOfNodes) {
        String translatedNode = "";
        for(final String node : listOfNodes.split(MessageConstants.NODE_PATH_SEPARATOR)) {
            if(node != null && !node.isEmpty()) {
                translatedNode += nodeNameMapping.getNodeNameInNumber(node) + MessageConstants.NODE_PATH_SEPARATOR;
            }
        }
        return translatedNode.substring(1, translatedNode.length() - MessageConstants.NODE_PATH_SEPARATOR.length()); // Strip off the last "->"
    }

    private void extractNodes() {
        for(final String linkWeight : linkWeightList) {
            final String [] nodeParts = linkWeight.split(" ");
            nodeNameMapping.addNodeMapping(nodeParts[0].trim());
            nodeNameMapping.addNodeMapping(nodeParts[1].trim());
        }
    }

    public NodeToNameMapping getNodeToNameMapping() {
        return nodeNameMapping;
    }

    public List<RoutingCache> getRoutingForAllNodes() {
        return routingCacheList;
    }
}
