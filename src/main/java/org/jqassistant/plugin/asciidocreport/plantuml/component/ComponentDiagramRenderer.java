package org.jqassistant.plugin.asciidocreport.plantuml.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractDiagramRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ComponentDiagramRenderer extends AbstractDiagramRenderer {

    private final SubGraphFactory subGraphFactory;

    public ComponentDiagramRenderer(SubGraphFactory subGraphFactory, RenderMode renderMode) {
        super(renderMode);
        this.subGraphFactory = subGraphFactory;
    }

    @Override
    protected void render(Result<? extends ExecutableRule> result, StringBuilder builder) throws ReportException {
        SubGraph subGraph = subGraphFactory.createSubGraph(result);
        Map<Long, Node> nodes = renderRootNodes(subGraph, builder, 0);
        builder.append('\n');
        renderRelationships(subGraph, builder, nodes);
    }

    /**
     * Render a {@link SubGraph}.
     *
     * @param graph
     *            The {@link SubGraph}.
     * @param builder
     *            The {@link StringBuilder} containing the PlantUML diagram.
     * @param level
     *            The current folder level.
     * @return A {@link Map} of all {@link Node}s in the {@link SubGraph} identified
     *         by their ids.
     */
    private Map<Long, Node> renderRootNodes(SubGraph graph, StringBuilder builder, int level) {
        Map<Long, Node> nodes = new LinkedHashMap<>();
        Node parentNode = graph.getParent();
        if (parentNode != null) {
            nodes.put(parentNode.getId(), parentNode);
            builder.append(indent(level)).append("folder ").append('"').append(escape(parentNode.getLabel())).append('"').append(" {\n");
            nodes.putAll(renderNodes(graph, builder, level + 1));
            nodes.putAll(renderSubGraphs(graph, builder, level + 1));
            builder.append(indent(level)).append("}\n");
        } else {
            nodes.putAll(renderNodes(graph, builder, level));
            nodes.putAll(renderSubGraphs(graph, builder, level));
        }
        return nodes;
    }

    /**
     * Render the {@link Node}s of a {@link SubGraph}.
     *
     * @param graph
     *            The {@link SubGraph}.
     * @param builder
     *            The {@link StringBuilder} containing the PlantUML diagram.
     * @param level
     *            The current folder level.
     * @return The {@link Map} of rendered {@link Node}s.
     */
    private Map<Long, Node> renderNodes(SubGraph graph, StringBuilder builder, int level) {
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (Node node : graph.getNodes().values()) {
            if (!node.equals(graph.getParent())) {
                nodes.put(node.getId(), node);
                builder.append(indent(level + 1)).append("component ").append('"').append(escape(node.getLabel())).append('"').append(' ');
                Set<String> labels = node.getLabels();
                if (!labels.isEmpty()) {
                    builder.append("<<");
                    builder.append(StringUtils.join(labels, " "));
                    builder.append(">>");
                }
                builder.append(" as ").append(getNodeId(node)).append('\n');
            }
        }
        return nodes;
    }

    /**
     * Render the {@link SubGraph}s of a {@link SubGraph}.
     *
     * @param graph
     *            The {@link SubGraph}.
     * @param builder
     *            The {@link StringBuilder} containing the PlantUML diagram.
     * @param level
     *            The current folder level.
     * @return The {@link Map} of rendered {@link Node}s.
     */
    private Map<Long, Node> renderSubGraphs(SubGraph graph, StringBuilder builder, int level) {
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (SubGraph subgraph : graph.getSubGraphs().values()) {
            nodes.putAll(renderRootNodes(subgraph, builder, level + 1));
        }
        return nodes;
    }

    /**
     * Render the relationships of a {@link SubGraph}.
     *
     * @param subGraph
     *            The {@link SubGraph}, for PlantUML the root graph.
     * @param builder
     *            The {@link StringBuilder} containing the PlantUML diagram.
     * @param nodes
     *            The {@link Node}s of the graph (including all {@link SubGraph}s.
     */
    private void renderRelationships(SubGraph subGraph, StringBuilder builder, Map<Long, Node> nodes) {
        Map<Long, Relationship> relationships = getRelationships(subGraph, nodes);
        for (Relationship relationship : relationships.values()) {
            Node startNode = relationship.getStartNode();
            Node endNode = relationship.getEndNode();
            String type = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, relationship.getType()).replace('_', ' ');
            builder.append(getNodeId(startNode)).append(" --> ").append(getNodeId(endNode)).append(" : ").append(type);
            if (isNotEmpty(relationship.getLabel())) {
                builder.append(" (").append(escape(relationship.getLabel())).append(")");
            }
            builder.append('\n');
        }
        builder.append('\n');
    }
}
