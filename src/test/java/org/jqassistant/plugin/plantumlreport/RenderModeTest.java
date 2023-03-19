package org.jqassistant.plugin.plantumlreport;

import com.buschmais.jqassistant.core.report.api.ReportException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderModeTest {

    @Test
    void detect() throws ReportException {
        assertThat(RenderMode.getRenderMode(null)).isIn(RenderMode.SMETANA, RenderMode.GRAPHVIZ);
    }

    @Test
    void smetana() throws ReportException {
        assertThat(RenderMode.getRenderMode("smetana")).isSameAs(RenderMode.SMETANA);
        assertThat(RenderMode.getRenderMode("SMETANA")).isSameAs(RenderMode.SMETANA);
    }

    @Test
    void elk() throws ReportException {
        assertThat(RenderMode.getRenderMode("elk")).isSameAs(RenderMode.ELK);
        assertThat(RenderMode.getRenderMode("ELK")).isSameAs(RenderMode.ELK);
    }

    @Test
    void graphviz() throws ReportException {
        assertThat(RenderMode.getRenderMode("graphviz")).isSameAs(RenderMode.GRAPHVIZ);
        assertThat(RenderMode.getRenderMode("GRAPHVIZ")).isSameAs(RenderMode.GRAPHVIZ);
    }

    @Test
    void unsupportedRenderer() {
        assertThrows(ReportException.class, () -> RenderMode.getRenderMode("invalidRenderer"));
    }
}
