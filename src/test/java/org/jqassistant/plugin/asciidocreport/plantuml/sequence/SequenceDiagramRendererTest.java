package org.jqassistant.plugin.asciidocreport.plantuml.sequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import org.jqassistant.plugin.asciidocreport.plantuml.ImageRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;

import org.jqassistant.plugin.asciidocreport.SubGraphTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.jqassistant.plugin.asciidocreport.SubGraphTestHelper.getRelationship;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link ImageRenderer}.
 */
@ExtendWith(MockitoExtension.class)
class SequenceDiagramRendererTest {

    @Mock
    private Descriptor p1 = mock(Descriptor.class);
    @Mock
    private Descriptor p2 = mock(Descriptor.class);
    @Mock
    private Descriptor p3 = mock(Descriptor.class);

    @Mock
    private Descriptor m1 = mock(Descriptor.class);
    @Mock
    private Descriptor m2 = mock(Descriptor.class);

    @Mock
    private SubGraphFactory subGraphFactory;

    private SequenceDiagramRenderer sequenceDiagramRenderer;

    @BeforeEach
    void setUp() throws ReportException {
        Node n1 = SubGraphTestHelper.getNode(1, "p1", "Java", "Method");
        doReturn(n1).when(subGraphFactory).toIdentifiable(p1);
        Node n2 = SubGraphTestHelper.getNode(2, "p2", "Java", "Method");
        doReturn(n2).when(subGraphFactory).toIdentifiable(p2);
        Node n3 = SubGraphTestHelper.getNode(3, "p3", "Java", "Method");
        doReturn(n3).when(subGraphFactory).toIdentifiable(p3);
        Mockito.doReturn(SubGraphTestHelper.getRelationship(1, n1, "INVOKES", n2)).when(subGraphFactory).toIdentifiable(m1);
        Mockito.doReturn(SubGraphTestHelper.getRelationship(2, n2, "INVOKES", n3)).when(subGraphFactory).toIdentifiable(m2);

        sequenceDiagramRenderer = new SequenceDiagramRenderer(subGraphFactory, RenderMode.GRAPHVIZ);
    }

    @Test
    void sequenceDiagramFromSequence() throws ReportException {
        verifySequenceDiagram(getSequenceResult());
    }

    @Test
    void sequenceDiagramFromParticipantsAndMessages() throws ReportException {
        verifySequenceDiagram(getParticipantsAndMessagesResult());
    }

    private void verifySequenceDiagram(Result<? extends ExecutableRule> result) throws ReportException {
        String componentDiagram = sequenceDiagramRenderer.renderDiagram(result);

        assertThat(componentDiagram, containsString("participant \"p1\" as n1 <<Java>> <<Method>>"));
        assertThat(componentDiagram, containsString("participant \"p2\" as n2 <<Java>> <<Method>>"));
        assertThat(componentDiagram, containsString("participant \"p3\" as n3 <<Java>> <<Method>>"));
        assertThat(componentDiagram, containsString("n1 -> n2 : INVOKES"));
        assertThat(componentDiagram, containsString("n2 -> n3 : INVOKES"));
    }

    private Result<? extends ExecutableRule> getSequenceResult() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("sequence", asList(p1, m1, p2));
        Map<String, Object> row2 = new HashMap<>();
        row2.put("sequence", asList(p1, m1, p2, m2, p3));
        List<Map<String, Object>> rows = asList(row1, row2);
        return Result.builder().columnNames(asList("sequence")).rows(rows).build();
    }

    private Result<? extends ExecutableRule> getParticipantsAndMessagesResult() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("participants", asList(p1, p2));
        row1.put("messages", asList(m1));
        Map<String, Object> row2 = new HashMap<>();
        row2.put("participants", asList(p1, p2, p3));
        row2.put("messages", asList(m1, m2));
        List<Map<String, Object>> rows = asList(row1, row2);
        return Result.builder().columnNames(asList("participants", "messages")).rows(rows).build();
    }

}
