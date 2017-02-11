package cs455.overlay.dijkstra;

import java.util.*;

public class ShortestPathGenerator {
    private int MAX_NODES;
    private int SOURCE;


    // graph to contain the weights
    private final int[][] graph  = new int[MAX_NODES][MAX_NODES] ;
    private final ConceptualNode[] nodes   = new ConceptualNode[MAX_NODES];

    private PriorityQueue<ConceptualNode> minHeap = new PriorityQueue<>(
            MAX_NODES, new Comparator<ConceptualNode>() {
        @Override
        public int compare(ConceptualNode o1, ConceptualNode o2) {
            return Integer.compare(o1.getDistance(), o2.getDistance());
        }
    }
    );


    ShortestPathGenerator(final int maxNodes, final int source) {
        this.MAX_NODES = maxNodes;
        this.SOURCE = source;

        for (int i = 0; i < MAX_NODES; i++) {
            for (int j = 0; j < MAX_NODES; j++) {
                if (i == j)
                    this.graph[i][j] = 0;  // Node to itself, the distance would be 0
                else
                    this.graph[i][j] = Integer.MAX_VALUE;
            }
        }
        initialize();
    }

    private void initialize() {
        // Initialize the nodes
        for (int i = 0; i < MAX_NODES; i++) {
            final ConceptualNode node = new ConceptualNode ();
            node.setId(i);
            node.setDistance(SOURCE == i ? 0: Integer.MAX_VALUE);
            node.setParent(-1);
            this.minHeap.add(node);
            this.nodes[i] = node;
        }
    }

    public void addEdge(int sourceNode, int destinationNode, int weight) {
        if (sourceNode < MAX_NODES && destinationNode < MAX_NODES) {
            this.graph[sourceNode][destinationNode] = weight;
            this.graph[destinationNode][sourceNode] = weight;
        }
    }

    public void buildPaths() {
        Set<ConceptualNode> nodeSet = new HashSet<>();
        while (!minHeap.isEmpty()) {
            ConceptualNode closestNodeInTheMap = minHeap.remove();
            nodeSet.add(closestNodeInTheMap);
            List<Integer> neighbors = new ArrayList<>();
            for (int v = 0; v < MAX_NODES; v++) {
                if (graph[closestNodeInTheMap.getId()][v] != Integer.MAX_VALUE) {
                    if (closestNodeInTheMap.getId() != v) {
                        neighbors.add(v);
                    }
                }
            }

            for (Integer v: neighbors) {
                relax(closestNodeInTheMap, nodes[v], graph[closestNodeInTheMap.getId()][v]);
            }
        }
    }

    private void relax(ConceptualNode u, ConceptualNode v, int weight) {
        if (v.getDistance() > u.getDistance()+ weight) {
            v.setDistance(u.getDistance()+ weight);
            v.setParent(u.getId());
            if (minHeap.contains(v)) {
                minHeap.remove(v);
                minHeap.add(v);
            }
        }
    }

    public List<RoutingCache> generatePathForTraffic() {
        final List<RoutingCache> routingForAllNodes = new ArrayList<>();

        for (int destinationNode = 0; destinationNode < MAX_NODES; destinationNode++) {
            if (destinationNode != SOURCE) {
                RoutingCache routingCache = new RoutingCache(Integer.toString(SOURCE), Integer.toString(destinationNode));
                Deque<Integer> stack = new ArrayDeque<>();
                ConceptualNode node = nodes[destinationNode];
                stack.addFirst(node.getId());
                routingCache.setNextHop(Integer.toString(node.getId()));
                while (node.getParent() != SOURCE) {
                    stack.addFirst(node.getParent());
                    node = nodes[node.getParent()];
                    routingCache.setNextHop(Integer.toString(node.getId()));
                }
                while (!stack.isEmpty()) {
                    Integer id = stack.removeFirst();
                    routingCache.addToPath(id.toString());
                }
                routingForAllNodes.add(routingCache);
            }
        }
        return routingForAllNodes;
    }
}
