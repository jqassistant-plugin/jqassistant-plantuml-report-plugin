package org.jqassistant.plugin.plantumlreport;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DiagramRendererTest {

    @Test
    void smetana() throws ReportException {
        String diagram = renderDiagram(RenderMode.SMETANA);
        assertThat(diagram).contains(RenderMode.SMETANA.getPragma());
    }

    @Test
    void elk() throws ReportException {
        String diagram = renderDiagram(RenderMode.ELK);
        assertThat(diagram).contains(RenderMode.ELK.getPragma());
    }

    @Test
    void graphviz() throws ReportException {
        String diagram = renderDiagram(RenderMode.GRAPHVIZ);
        assertThat(diagram).doesNotContain(RenderMode.SMETANA.getPragma());
        assertThat(diagram).doesNotContain(RenderMode.ELK.getPragma());
    }

    private String renderDiagram(RenderMode renderMode) throws ReportException {
        AbstractDiagramRenderer renderer = new AbstractDiagramRenderer(renderMode) {
            @Override
            protected void render(Result<? extends ExecutableRule> result, StringBuilder builder) {
            }
        };
        return renderer.renderDiagram(mock(Result.class));
    }
}
