package org.jqassistant.plugin.asciidocreport.plantuml;

import java.io.File;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.rule.api.model.Concept;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link ImageRenderer}.
 */
class ImageRendererTest {

    private ImageRenderer imageRenderer = new ImageRenderer();

    @Test
    void renderDiagramAsSvg() throws ReportException {
        File file = renderDiagram("svg", "svg");

        assertThat(file.exists(), equalTo(true));
    }

    @Test
    void renderDiagramAsPng() throws ReportException {
        File file = renderDiagram("png", "png");

        assertThat(file.exists(), equalTo(true));
    }

    @Test
    void renderDiagramDefaultFormat() throws ReportException {
        File file = renderDiagram(null, "svg");

        assertThat(file.exists(), equalTo(true));
        assertThat(file.getName().endsWith(".svg"), equalTo(true));
    }

    @Test
    void renderDiagramEmptyFormat() {
        assertThrows(ReportException.class, () -> renderDiagram("", ""));
    }

    @Test
    void renderDiagramUnknownFormat() {
        assertThrows(ReportException.class, () -> renderDiagram("notExisting", ""));
    }

    private File renderDiagram(String format, String expectedFormat) throws ReportException {
        Concept concept = Concept.builder().id("test:plantuml").build();
        File directory = new File("target");
        directory.mkdirs();
        File file = new File(directory, "test_plantuml." + expectedFormat);
        if (file.exists()) {
            assertThat(file.delete(), equalTo(true));
        }
        String componentDiagram = "@startuml\n" + "component MyComponent\n" + "@enduml";
        imageRenderer.renderDiagram(componentDiagram, concept, directory, format);
        return file;
    }

}
