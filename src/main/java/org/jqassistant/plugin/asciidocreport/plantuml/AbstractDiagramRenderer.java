package org.jqassistant.plugin.asciidocreport.plantuml;

import java.util.LinkedHashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import org.apache.commons.lang3.StringUtils;

import static java.util.Arrays.fill;

public abstract class AbstractDiagramRenderer {

    private final RenderMode renderMode;

    /**
     * Constructor.
     *
     * @param renderMode
     *            The {@link RenderMode} to use.
     */
    protected AbstractDiagramRenderer(RenderMode renderMode) {
        this.renderMode = renderMode;
    }

    /**
     * Render a diagram from the given {@link SubGraph}.
     *
     * <p>
     * The {@link SubGraph} may contain {@link Node}s, {@link Relationship}s and
     * {@link SubGraph}s. The latter are diagram specific and will be rendered as
     * folders.
     * </p>
     *
     * @param result
     *            The {@link SubGraph}.
     * @return The {@link String} representation of the PlantUML diagram.
     * @throws ReportException
     *             If the renderer fails.
     */
    public final String renderDiagram(Result<? extends ExecutableRule> result) throws ReportException {
        StringBuilder plantumlBuilder = new StringBuilder();
        plantumlBuilder.append("@startuml").append('\n');
        plantumlBuilder.append("skinparam componentStyle uml2").append('\n');
        plantumlBuilder.append(renderMode.getPragma()).append('\n');
        render(result, plantumlBuilder);
        plantumlBuilder.append("@enduml").append('\n');
        return plantumlBuilder.toString();
    }

    protected abstract void render(Result<? extends ExecutableRule> result, StringBuilder builder) throws ReportException;

    /**
     * Creates a white-space based indent from the given folder level.
     *
     * @param level
     *            The level.
     * @return The indent.
     */
    protected final String indent(int level) {
        char[] indent = new char[level * 2];
        fill(indent, ' ');
        return new String(indent);
    }

    /**
     * Generate a unique id {@link String} for a {@link Node}.
     *
     * @param node
     *            The {@link Node}.
     * @return The id.
     */
    protected final String getNodeId(Node node) {
        String id = "n" + node.getId();
        return id.replaceAll("-", "_");
    }

    /**
     * Collect all {@link Relationship}s of a {@link SubGraph} and all its children.
     *
     * <p>
     * Only those {@link Relationship}s are considered where both start and end
     * {@link Node} are also part of the {@link SubGraph} and its children.
     * </p>
     *
     * @param graph
     *            The {@link SubGraph}.
     * @param nodes
     *            The {@link Node}s of the SubGraph and its children.
     * @return A {@link Map} of {@link Relationship}s idendified by their ids.
     */
    protected final Map<Long, Relationship> getRelationships(SubGraph graph, Map<Long, Node> nodes) {
        Map<Long, Relationship> relationships = new LinkedHashMap<>();
        for (Map.Entry<Long, Relationship> entry : graph.getRelationships().entrySet()) {
            Relationship relationship = entry.getValue();
            if (nodes.containsKey(relationship.getStartNode().getId()) && nodes.containsKey(relationship.getEndNode().getId())) {
                relationships.put(entry.getKey(), relationship);
            }
        }
        for (SubGraph subgraph : graph.getSubGraphs().values()) {
            relationships.putAll(getRelationships(subgraph, nodes));
        }
        return relationships;
    }

    /**
     * Returns an escaped label.
     *
     * @param label
     *     The label
     * @return The escaped label.
     */
    protected final String escape(String label) {
        // handle double quotes
        return StringUtils.replace(label, "\"", "<U+0022>");
    }

}
