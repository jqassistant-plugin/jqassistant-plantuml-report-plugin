package org.jqassistant.plugin.asciidocreport.plantuml.component;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import org.jqassistant.plugin.asciidocreport.plantuml.ImageRenderer;
import org.jqassistant.plugin.asciidocreport.plantuml.RenderMode;

import org.jqassistant.plugin.asciidocreport.SubGraphTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.jqassistant.plugin.asciidocreport.SubGraphTestHelper.getRelationship;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for the {@link ImageRenderer}.
 */
@ExtendWith(MockitoExtension.class)
class ComponentDiagramRendererTest {

    @Mock
    private Result<?> result;

    @Mock
    private SubGraphFactory subGraphFactory;

    private ComponentDiagramRenderer componentDiagramRenderer;

    @BeforeEach
    void setUp() {
        componentDiagramRenderer = new ComponentDiagramRenderer(subGraphFactory, RenderMode.GRAPHVIZ);
    }

    @Test
    void componentDiagram() throws ReportException {
        doReturn(getSubGraph()).when(subGraphFactory).createSubGraph(result);

        String componentDiagram = componentDiagramRenderer.renderDiagram(result);

        assertThat(componentDiagram, containsString("component \"a1\" <<Artifact File>> as n1"));
        assertThat(componentDiagram, containsString("component \"a2\" <<Artifact File>> as n2"));
        assertThat(componentDiagram, containsString("component \"a3\" <<Artifact File>> as n3"));
        assertThat(componentDiagram, containsString("n1 --> n2 : depends on (weight:3)"));
        assertThat(componentDiagram, not(containsString("component \"a4\" <<Artifact File>> as n4")));
        assertThat(componentDiagram, not(containsString("n1 --> n4 : DEPENDS_ON")));
    }

    @Test
    void componentDiagramInFolder() throws ReportException {
        Node rootFolder = SubGraphTestHelper.getNode(-1, "a0", "Artifact", "File", "Container");
        Node a1 = SubGraphTestHelper.getNode(1, "a1", "Artifact", "File");
        SubGraph rootGraph = new SubGraph();
        rootGraph.setId(-1);
        rootGraph.getNodes().put(a1.getId(), a1);
        rootGraph.getNodes().put(rootFolder.getId(), rootFolder); // current behavior of SubGraphFactory
        rootGraph.setParent(rootFolder);
        Node nestedFolder = SubGraphTestHelper.getNode(2, "a2", "Artifact", "File", "Container");
        Node a2 = SubGraphTestHelper.getNode(3, "a3", "Artifact", "File");
        SubGraph nestedGraph = new SubGraph();
        nestedGraph.setId(-2);
        nestedGraph.setParent(nestedFolder);
        nestedGraph.getNodes().put(nestedFolder.getId(), nestedFolder);
        nestedGraph.getNodes().put(a2.getId(), a2);
        Relationship a2DependsOnA1 = SubGraphTestHelper.getRelationship(1, a2, "DEPENDS_ON", a1);
        nestedGraph.getRelationships().put(a2DependsOnA1.getId(), a2DependsOnA1);
        rootGraph.getSubGraphs().put(nestedGraph.getId(), nestedGraph);

        doReturn(rootGraph).when(subGraphFactory).createSubGraph(result);

        String componentDiagram = componentDiagramRenderer.renderDiagram(result);

        assertThat(componentDiagram, containsString("folder \"a0\" {\n" + "    component \"a1\" <<Artifact File>> as n1\n" + "    folder \"a2\" {\n"
                + "        component \"a3\" <<Artifact File>> as n3\n" + "    }\n" + "}"));
        assertThat(componentDiagram, containsString("n3 --> n1 : depends on"));
    }

    @Test
    void extraCharacters() throws ReportException {
        Node a1 = SubGraphTestHelper.getNode(1, "\"a1\"", "Artifact", "File");
        Node a2 = SubGraphTestHelper.getNode(2, "\"a2\"", "Artifact", "File");
        Relationship a1DependsOnA2 = SubGraphTestHelper.getRelationship(1, a1, "DEPENDS_ON", a2, "\"weight:3\"");
        SubGraph subGraph = new SubGraph();
        subGraph.getNodes().put(a1.getId(), a1);
        subGraph.getNodes().put(a2.getId(), a2);
        subGraph.getRelationships().put(a1DependsOnA2.getId(), a1DependsOnA2);
        doReturn(subGraph).when(subGraphFactory).createSubGraph(result);

        String componentDiagram = componentDiagramRenderer.renderDiagram(result);

        assertThat(componentDiagram, containsString("component \"<U+0022>a1<U+0022>\" <<Artifact File>> as n1"));
        assertThat(componentDiagram, containsString("component \"<U+0022>a2<U+0022>\" <<Artifact File>> as n2"));
        assertThat(componentDiagram, containsString("n1 --> n2 : depends on (<U+0022>weight:3<U+0022>)"));
    }

    private SubGraph getSubGraph() {
        Node a1 = SubGraphTestHelper.getNode(1, "a1", "Artifact", "File");
        Node a2 = SubGraphTestHelper.getNode(2, "a2", "Artifact", "File");
        Node a3 = SubGraphTestHelper.getNode(3, "a3", "Artifact", "File");
        // a4 is not part of the created sub-graph
        Node a4 = SubGraphTestHelper.getNode(4, "a4", "Artifact", "File");
        Relationship a1DependsOnA2 = SubGraphTestHelper.getRelationship(1, a1, "DEPENDS_ON", a2, "weight:3");
        Relationship a1DependsOnA4 = SubGraphTestHelper.getRelationship(2, a1, "DEPENDS_ON", a4);
        SubGraph subGraph = new SubGraph();
        subGraph.getNodes().put(a1.getId(), a1);
        subGraph.getNodes().put(a2.getId(), a2);
        subGraph.getNodes().put(a3.getId(), a3);
        subGraph.getRelationships().put(a1DependsOnA2.getId(), a1DependsOnA2);
        subGraph.getRelationships().put(a1DependsOnA4.getId(), a1DependsOnA4);
        return subGraph;
    }
}
