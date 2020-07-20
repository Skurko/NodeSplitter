package splitter.results;

import org.neo4j.graphdb.Node;

public class SplitNodeResult {
    public final Node node;

    public SplitNodeResult(Node node) {
        this.node = node;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SplitNodeResult))
            return false;

        return this == obj || this.node.equals(((SplitNodeResult)obj).node);
    }

    @Override
    public int hashCode() {
        return this.node.hashCode();
    }
}
