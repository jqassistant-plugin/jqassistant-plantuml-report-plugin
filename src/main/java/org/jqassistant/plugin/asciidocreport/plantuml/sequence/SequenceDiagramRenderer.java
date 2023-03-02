package org.jqassistant.plugin.asciidocreport.plantuml.sequence;

import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.Identifiable;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import org.jqassistant.plugin.asciidocreport.plantuml.AbstractDiagramRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;

public class SequenceDiagramRenderer extends AbstractDiagramRenderer {

    public static final String COLUMN_SEQUENCE = "sequence";
    public static final String COLUMN_PARTICIPANTS = "participants";
    public static final String COLUMN_MESSAGES = "messages";

    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceDiagramRenderer.class);

    private final SubGraphFactory subGraphFactory;

    SequenceDiagramRenderer(SubGraphFactory subGraphFactory, RenderMode renderMode) {
        super(renderMode);
        this.subGraphFactory = subGraphFactory;
    }

    @Override
    protected void render(Result<? extends ExecutableRule> result, StringBuilder builder) throws ReportException {
        List<Map<String, Object>> rows = result.getRows();
        Set<Node> participants = new LinkedHashSet<>();
        Set<Relationship> messages = new LinkedHashSet<>();
        convertResult(rows, participants, messages);
        LOGGER.info("Rendering sequence diagram with {} participants and {} messages.", participants.size(), messages.size());
        renderParticipants(participants, builder);
        builder.append('\n');
        renderRelationships(messages, builder);
    }

    private void convertResult(List<Map<String, Object>> rows, Set<Node> participants, Set<Relationship> messages) throws ReportException {
        for (Map<String, Object> row : rows) {
            List<?> nodes;
            List<?> relationships;
            List<?> sequence = (List<?>) row.getOrDefault(COLUMN_SEQUENCE, emptyList());
            if (!sequence.isEmpty()) {
                for (Object value : sequence) {
                    Identifiable identifiable = subGraphFactory.toIdentifiable(value);
                    if (identifiable instanceof Node) {
                        participants.add((Node) identifiable);
                    } else if (identifiable instanceof Relationship) {
                        messages.add((Relationship) identifiable);
                    }
                }
            } else {
                nodes = (List<?>) row.getOrDefault(COLUMN_PARTICIPANTS, emptyList());
                participants.addAll(convert(nodes, subGraphFactory));
                relationships = (List<?>) row.getOrDefault(COLUMN_MESSAGES, emptyList());
                messages.addAll(convert(relationships, subGraphFactory));
            }
        }
    }

    private <T extends Identifiable> List<T> convert(List<?> elements, SubGraphFactory subGraphFactory) throws ReportException {
        List<T> result = new ArrayList<>(elements.size());
        for (Object element : elements) {
            result.add(subGraphFactory.toIdentifiable(element));
        }
        return result;
    }

    /**
     * Render the {@link Node}s of a {@link SubGraph}.
     *
     * @param participants
     *            The {@link Node}s to be rendered as participants.
     * @param builder
     *            The {@link StringBuilder} containing the PlantUML diagram.
     * @return The {@link Map} of rendered {@link Node}s.
     */
    private void renderParticipants(Collection<Node> participants, StringBuilder builder) {
        for (Node participant : participants) {
            builder.append("participant ").append('"').append(participant.getLabel()).append('"').append(" as ").append(getNodeId(participant));
            Set<String> labels = participant.getLabels();
            for (String label : labels) {
                builder.append(" <<");
                builder.append(label);
                builder.append(">>");
            }
            builder.append('\n');
        }
    }

    /**
     * Render the relationships of a {@link SubGraph}.
     *
     * @param messages
     *            The {@link Relationship}s to be rendered as messages.
     * @param builder
     *            The {@link StringBuilder} containing the PlantUML diagram.
     */
    private void renderRelationships(Collection<Relationship> messages, StringBuilder builder) {
        for (Relationship relationship : messages) {
            Node startNode = relationship.getStartNode();
            Node endNode = relationship.getEndNode();
            builder.append(getNodeId(startNode)).append(" -> ").append(getNodeId(endNode)).append(" : ").append(relationship.getType()).append('\n');
        }
        builder.append('\n');
    }
}
