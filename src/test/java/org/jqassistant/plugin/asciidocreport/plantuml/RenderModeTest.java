package org.jqassistant.plugin.asciidocreport.plantuml;

import com.buschmais.jqassistant.core.report.api.ReportException;

import org.junit.jupiter.api.Test;

import static org.jqassistant.plugin.asciidocreport.plantuml.RenderMode.ELK;
import static org.jqassistant.plugin.asciidocreport.plantuml.RenderMode.GRAPHVIZ;
import static org.jqassistant.plugin.asciidocreport.plantuml.RenderMode.SMETANA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderModeTest {

    @Test
    void detect() throws ReportException {
        assertThat(RenderMode.getRenderMode(null)).isIn(SMETANA, GRAPHVIZ);
    }

    @Test
    void smetana() throws ReportException {
        assertThat(RenderMode.getRenderMode("smetana")).isSameAs(SMETANA);
        assertThat(RenderMode.getRenderMode("SMETANA")).isSameAs(SMETANA);
    }

    @Test
    void elk() throws ReportException {
        assertThat(RenderMode.getRenderMode("elk")).isSameAs(ELK);
        assertThat(RenderMode.getRenderMode("ELK")).isSameAs(ELK);
    }

    @Test
    void graphviz() throws ReportException {
        assertThat(RenderMode.getRenderMode("graphviz")).isSameAs(GRAPHVIZ);
        assertThat(RenderMode.getRenderMode("GRAPHVIZ")).isSameAs(GRAPHVIZ);
    }

    @Test
    void unsupportedRenderer() {
        assertThrows(ReportException.class, () -> {
            RenderMode.getRenderMode("invalidRenderer");
        });
    }

}
