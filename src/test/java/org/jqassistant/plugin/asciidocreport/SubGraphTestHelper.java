package org.jqassistant.plugin.asciidocreport;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;

import static java.util.Arrays.asList;

public final class SubGraphTestHelper {

    public static Node getNode(long id, String label, String... labels) {
        Node node = new Node();
        node.setId(id);
        node.setLabel(label);
        node.getLabels().addAll(asList(labels));
        return node;
    }

    public static Relationship getRelationship(long id, Node start, String type, Node end) {
        return getRelationship(id, start, type, end, "");
    }

    public static Relationship getRelationship(long id, Node start, String type, Node end, String label) {
        Relationship relationship = new Relationship();
        relationship.setId(id);
        relationship.setStartNode(start);
        relationship.setEndNode(end);
        relationship.setType(type);
        relationship.setLabel(label);
        return relationship;
    }

}
