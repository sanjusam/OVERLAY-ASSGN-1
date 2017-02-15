package cs455.overlay.dijkstra;

public class ConceptualNode {  //Each messaging node is represented in numbers.  there is another class NodeToNameMapping, which holds the mapping.
    private int id;
    private int distance;
    private int parent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptualNode node = (ConceptualNode) o;
        return id == node.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
