package splitter;

import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.procedure.*;
import splitter.config.SplitNodeConfiguration;
import splitter.results.SplitNodeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SplitNode {
    @Context
    public GraphDatabaseService db;

    @Procedure(mode = Mode.WRITE)
    @Description("splitter.splitNodes([node1, node2]], {startIndex:0, indexProperty: 'Index', relationshipTypes: ['Type1', 'Type2']}) Split each node from list into multiple nodes based on relationships with specified types")
    public Stream<SplitNodeResult> splitNodes(@Name("nodes") List<Node> nodes, @Name("configuration") Map<String,Object> configuration) {
        SplitNodeConfiguration config = SplitNodeConfiguration.build(configuration);
        return nodes.stream().flatMap(node -> splitNode(node, config).stream()).filter(Objects::nonNull).map(SplitNodeResult::new);
    }

    private List<Node> splitNode(Node node, SplitNodeConfiguration config) {
        if (node == null || node.getDegree() == 0)
            return null;

        Transaction tx = db.beginTx();
        tx.acquireWriteLock(node);

        ArrayList<Relationship> ignoreIncomingRelationships = new ArrayList<>();
        ArrayList<Relationship> incomingRelationships = new ArrayList<>();
        node.getRelationships(Direction.INCOMING).forEach(relationship -> {
            tx.acquireWriteLock(relationship.getOtherNode(node));
            if (config.getRelationshipTypes().contains(relationship.getType().name())) {
                incomingRelationships.add(relationship);
            } else {
                ignoreIncomingRelationships.add(relationship);
            }
        });

        ArrayList<Relationship> ignoreOutgoingRelationships = new ArrayList<>();
        ArrayList<Relationship> outgoingRelationships = new ArrayList<>();
        node.getRelationships(Direction.OUTGOING).forEach(relationship -> {
            tx.acquireWriteLock(relationship.getOtherNode(node));
            if (config.getRelationshipTypes().contains(relationship.getType().name())) {
                outgoingRelationships.add(relationship);
            } else {
                ignoreOutgoingRelationships.add(relationship);
            }
        });

        String indexProperty = config.getIndexPropertyName();
        int index = config.getStartIndex();
        List<Node> enterNodes = createSplitNodes(node, incomingRelationships, indexProperty, index, Direction.INCOMING);
        List<Node> exitNodes = createSplitNodes(node, outgoingRelationships, indexProperty, index + enterNodes.size(), Direction.OUTGOING);

        for (Node enterNode : enterNodes) {
            Relationship relationship = Iterables.first(enterNode.getRelationships());
            for (Node exitNode : exitNodes) {
                createRelationship(enterNode, exitNode, relationship);
            }
        }
        repairRelationships(enterNodes, exitNodes, ignoreIncomingRelationships, ignoreOutgoingRelationships);


        detachDeleteNode(node);
        return Stream.concat(enterNodes.stream(), exitNodes.stream()).collect(Collectors.toList());
    }

    private void createRelationship(Node from, Node to, Relationship source) {
        Relationship relationship = from.createRelationshipTo(to, source.getType());
        source.getAllProperties().forEach(relationship::setProperty);
    }

    private void detachDeleteNode(Node node) {
        node.getRelationships().forEach(Relationship::delete);
        node.delete();
    }

    private Relationship createRelationship(Node targetNode, Node otherNode, Direction direction, RelationshipType type) {
        if (direction == Direction.INCOMING) {
            return otherNode.createRelationshipTo(targetNode, type);
        } else {
            return targetNode.createRelationshipTo(otherNode, type);
        }
    }

    private List<Node> createSplitNodes(Node source, List<Relationship> relationships, String indexProperty, int startIndex, Direction direction) {
        Label[] labels = Iterables.asArray(Label.class, source.getLabels());
        ArrayList<Node> splitNodes = new ArrayList<>(relationships.size());
        for (Relationship relationship : relationships) {
            Node splitNode = createSplitNode(labels, indexProperty, startIndex);
            copyNodeProperties(source, splitNode);
            if (direction == Direction.INCOMING) {
                Node otherNode = relationship.getStartNode();
                createRelationship(otherNode, splitNode, relationship);
            } else {
                Node otherNode = relationship.getEndNode();
                createRelationship(splitNode, otherNode, relationship);
            }
            startIndex++;
            splitNodes.add(splitNode);
        }
        return splitNodes;
    }

    private Node createSplitNode(Label[] labels, String indexPropertyName, int index) {
        Node node = db.createNode(labels);
        if (indexPropertyName != null) { node.setProperty(indexPropertyName, index); }
        return node;
    }

    private void copyNodeProperties(Node source, Node target) {
        source.getAllProperties().forEach(target::setProperty);
    }

    private void repairRelationships(List<Node> enterNodes, List<Node> exitNodes, List<Relationship> incoming, List<Relationship> outgoing) {
        enterNodes.forEach(enterNode -> repairRelationships(enterNode, incoming, Direction.INCOMING));
        enterNodes.forEach(enterNode -> repairRelationships(enterNode, outgoing, Direction.OUTGOING));
        exitNodes.forEach(exitNode -> repairRelationships(exitNode, incoming, Direction.INCOMING));
        exitNodes.forEach(exitNode -> repairRelationships(exitNode, outgoing, Direction.OUTGOING));
    }

    private void repairRelationships(Node node, List<Relationship> relationships, Direction direction) {
        if (direction == Direction.BOTH) return;
        relationships.forEach(relationship -> {
            Node otherNode = direction == Direction.INCOMING ? relationship.getStartNode() : relationship.getEndNode();
            Relationship repairedRelationship = createRelationship(node, otherNode, direction, relationship.getType());
            relationship.getAllProperties().forEach(repairedRelationship::setProperty);
        });
    }
}
